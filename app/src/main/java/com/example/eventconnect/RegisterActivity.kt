package com.example.eventconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : AppCompatActivity() {
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var localeHelper: LocaleHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellidos)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etPassword = findViewById(R.id.etPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnRegistrar.isEnabled = false

        validarCamposRellenos()

        localeHelper = LocaleHelper()

        val savedLanguage = LocaleHelper.getLanguage(this)
        savedLanguage?.let { LocaleHelper.setLocale(this, it) }

        val btnEnglish = findViewById<ImageView>(R.id.flagEnImageViewRegister)
        val btnSpanish = findViewById<ImageView>(R.id.flagEsImageViewRegister)

        if (btnEnglish != null && btnSpanish != null) {
            btnEnglish.setOnClickListener {
                LocaleHelper.persist(this, "en")
                LocaleHelper.setLocale(this, "en")
                recreate()
            }

            btnSpanish.setOnClickListener {
                LocaleHelper.persist(this, "es")
                LocaleHelper.setLocale(this, "es")
                recreate()
            }
        } else {
            // Log o manejo de error si los ImageView no se encuentran
        }

        // Registro con correo y contraseña
        firebaseAuth = FirebaseAuth.getInstance()

        btnRegistrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()

            if (validateFields(email, password, telefono)) {
                registerUser(email, password)
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            showToast(R.string.msg_fields_required)
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast(R.string.msg_successful_registration)
                    sendEmailVerification()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    // Manejar el error específico de correo electrónico ya en uso
                    if (task.exception != null) {
                        Log.e("FirebaseAuthException", task.exception.toString())
                    }

                    if (task.exception is FirebaseAuthUserCollisionException) {
                        showToast(R.string.msg_user_already_exists)
                    } else {
                        Log.d("FirebaseAuthException", "Error desconocido")
                        showToast(R.string.msg_registration_error)
                    }
                }
            }
    }

    private fun sendEmailVerification() {
        val user = firebaseAuth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast(R.string.msg_verification_email_sent)
                    // Puedes redirigir al usuario a una actividad de verificación o simplemente mostrar un mensaje
                } else {
                    Log.e("FirebaseAuthException", "Error sending verification email: ${task.exception}")
                    showToast(R.string.msg_verification_email_error)
                }
            }
    }

    private fun validateFields(email: String, password: String, telefono: String): Boolean {
        val isGmail = EmailValidator.isValidEmail(email)
        val isValidPassword = PasswordValidator.isValidPassword(password)
        val isValidPhone = PhoneValidator.isValidPhone(telefono)

        // Mostrar mensajes de error en los TextInputLayout correspondientes
        mostrarError(isGmail, etEmail, R.string.msg_invalid_email)
        mostrarError(isValidPassword, etPassword, R.string.msg_invalid_password)
        mostrarError(isValidPhone, etTelefono, R.string.msg_invalid_phone)

        return isGmail && isValidPassword && isValidPhone
    }

    private fun mostrarError(valido: Boolean, editText: EditText, mensajeError: Int) {
        val textInputLayout = editText.parent.parent as? TextInputLayout
        textInputLayout?.error = if (!valido) getString(mensajeError) else null
    }

    private fun validarCamposRellenos() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se requiere implementación
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val nombre = etNombre.text.toString().trim()
                val apellido = etApellido.text.toString().trim()
                val telefono = etTelefono.text.toString().trim()

                btnRegistrar.isEnabled = email.isNotEmpty() &&
                        password.isNotEmpty() && nombre.isNotEmpty() &&
                        apellido.isNotEmpty() && telefono.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
                // No se requiere implementación
            }
        }

        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
        etNombre.addTextChangedListener(textWatcher)
        etApellido.addTextChangedListener(textWatcher)
        etTelefono.addTextChangedListener(textWatcher)
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}

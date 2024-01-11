package com.example.eventconnect

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ForgotPassActivity : AppCompatActivity() {

    private lateinit var btRestablecerPass: Button
    private lateinit var etEmailRestablecer: TextInputEditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var localeHelper: LocaleHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)

        // Inicializar Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance()

        btRestablecerPass = findViewById(R.id.btRestablecerPass)
        etEmailRestablecer = findViewById(R.id.etEmailRestablecerPass)
        btRestablecerPass.setOnClickListener {
            val email = etEmailRestablecer.text.toString().trim()

            if (validateFields(email)) {
                restablecerPass(email)
            }

        }

        localeHelper = LocaleHelper()

        val savedLanguage = LocaleHelper.getLanguage(this)
        savedLanguage?.let { LocaleHelper.setLocale(this, it) }

        val btnEnglish = findViewById<ImageView>(R.id.flagEnImageViewForgotPass)
        val btnSpanish = findViewById<ImageView>(R.id.flagEsImageViewForgotPass)

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
    }

    private fun restablecerPass(email: String) {
        if (email.isEmpty()) {
            showToast(R.string.enter_your_email_2)
            return
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(R.string.reset_email_sent)
                } else {
                    showToast(R.string.reset_email_error)
                }
            }
    }

    private fun validateFields(email: String): Boolean {
        val isGmail = EmailValidator.isValidEmail(email)

        mostrarError(isGmail, etEmailRestablecer, R.string.msg_invalid_email)

        return isGmail
    }

    private fun mostrarError(valido: Boolean, editText: EditText, mensajeError: Int) {
        val textInputLayout = editText.parent.parent as? TextInputLayout
        textInputLayout?.error = if (!valido) getString(mensajeError) else null
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}
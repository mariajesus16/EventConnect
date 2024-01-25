package com.example.eventconnect

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var localeHelper: LocaleHelper
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 123

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)

        // Inicializa Firebase Authentication
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("user_pref", Context.MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val registerText = findViewById<TextView>(R.id.tRegistro)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val forgotPass = findViewById<TextView>(R.id.PassOlvidada)

        forgotPass.setOnClickListener {
            val passOlvidada = Intent(this@LoginActivity, ForgotPassActivity::class.java)
            startActivity(passOlvidada)
        }

        localeHelper = LocaleHelper()

        val savedLanguage = LocaleHelper.getLanguage(this)
        savedLanguage?.let { LocaleHelper.setLocale(this, it) }

        val btnEnglish = findViewById<ImageView>(R.id.flagEnImageViewLogin)
        val btnSpanish = findViewById<ImageView>(R.id.flagEsImageViewLogin)

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

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                showToast(R.string.msg_fields_required)
            }
        }

        // Verifica si el usuario ya está autenticado y tiene un ID guardado
        val savedUserId = sharedPreferences.getString("userId", null)
        if (auth.currentUser != null && savedUserId != null) {
            // El usuario está autenticado y tiene un ID guardado, abre directamente el menú
            val menu = Intent(this@LoginActivity, MenuActivity::class.java)
            startActivity(menu)
            finish()
        }

        registerText.setOnClickListener {
            val registro = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(registro)
        }

        // Configuración del inicio de sesión con Google
        configureGoogleSignIn()

        val btnSignInGoogle = findViewById<SignInButton>(R.id.btnSignInGoogle)
        btnSignInGoogle.setOnClickListener {
            signInWithGoogle()
        }

        btnSignInGoogle.setSize(SignInButton.SIZE_WIDE);
        btnSignInGoogle.setSize(SignInButton.SIZE_ICON_ONLY);
        btnSignInGoogle.setColorScheme(SignInButton.COLOR_DARK);
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In fue exitoso, autentica con Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In falló, maneja el error
                Log.w(TAG, "Google sign in failed", e)
                showToast(R.string.msg_google_sign_in_failed)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión con Google exitoso, realiza acciones adicionales si es necesario
                    val user = auth.currentUser
                    user?.uid?.let { userId ->
                        // Guardar el userId en las preferencias compartidas
                        val sharedPreferences: SharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putString("userId", userId)
                        editor.apply()
                    }
                    // Obtener información del usuario de Google
                    val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
                    val nombre = googleSignInAccount?.givenName
                    val apellido = googleSignInAccount?.familyName
                    val gmail = googleSignInAccount?.email

                    // Guardar el nombre y el apellido en Firebase Realtime Database
                    if (user != null && nombre != null && apellido != null && gmail != null) {
                        guardarNombreApellidoEnFirebase(user.uid, nombre, apellido, gmail)
                    }

                    showToast(R.string.msg_successful_google_sign_in)

                    // Puedes redirigir al usuario a la siguiente actividad aquí
                    val menu = Intent(this@LoginActivity, MenuActivity::class.java)
                    startActivity(menu)
                    finish()
                } else {
                    // Si el inicio de sesión con Firebase falla, maneja el error
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showToast(R.string.msg_google_sign_in_failed)
                }
            }
    }
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // Inicio de sesión exitoso y correo electrónico verificado
                        showToast(R.string.msg_successful_login)

                        // Guardar el ID del usuario en SharedPreferences
                        sharedPreferences.edit().putString("userId", user.uid).apply()

                        // Puedes redirigir al usuario a la siguiente actividad aquí
                        val menu = Intent(this@LoginActivity, MenuActivity::class.java)
                        startActivity(menu)

                        finish()

                    } else {
                        // Usuario no verificado por correo electrónico
                        showToast(R.string.msg_verify_email_for_login)
                    }
                } else {
                    // Error en el inicio de sesión
                    showToast(R.string.msg_login_error)
                }
            }
    }
    private fun guardarNombreApellidoEnFirebase(userId: String, nombre: String, apellido: String, gmail: String) {
        val databaseReference = FirebaseDatabase.getInstance("https://eventconnect-150ed-default-rtdb.europe-west1.firebasedatabase.app/").reference
        val usuarioRef = databaseReference.child("usuarios").child(userId)

        // Guardar nombre y apellido en Firebase Realtime Database
        usuarioRef.child("nombrePerfil").setValue("$nombre $apellido")
        usuarioRef.child("correo").setValue(gmail)
            .addOnSuccessListener {
                // Éxito al guardar el nombre y el apellido
            }
            .addOnFailureListener { e ->
                // Error al guardar el nombre y el apellido
                Log.e("FirebaseDatabaseError", "Error al guardar nombre y apellido: $e")
            }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}

package com.example.eventconnect.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.eventconnect.MainActivity
import com.example.eventconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminMenuActivity : AppCompatActivity(), AdminAddEventFragment.ActivityListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Configurar el BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(AdminHomeFragment())
                    true
                }

                R.id.add -> {
                    loadFragment(AdminAddEventFragment())
                    true
                }

                R.id.close_sesion -> {
                    mostrarDialogoConfirmacionCerrarSesion()
                    true
                }
                // Agrega más fragmentos según tus necesidades
                else -> false
            }
        }

        // Cargar el fragmento inicial
        loadFragment(AdminHomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun mostrarDialogoConfirmacionCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.logout))
        builder.setMessage(getString(R.string.confirm_logout))
        builder.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
            cerrarSesion()
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.not)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun cerrarSesion() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val userId = sharedPreferences.getString("userId", "") ?: ""
        // Borrar la caché local del userId en SharedPreferences
        sharedPreferences.edit().remove(userId).apply()

        // Restablecer userId a una cadena vacía
        sharedPreferences.edit().putString("userId", "").apply()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        showToast(getString(R.string.logout_success))
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun restartActivity() {
        // Reinicia la actividad
        finish()
        startActivity(intent)
    }
}



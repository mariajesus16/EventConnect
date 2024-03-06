package com.example.eventconnect.admin

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
                R.id.settings -> {
                    loadFragment(AdminSettingsFragment())
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
    override fun restartActivity() {
        // Reinicia la actividad
        finish()
        startActivity(intent)
    }}



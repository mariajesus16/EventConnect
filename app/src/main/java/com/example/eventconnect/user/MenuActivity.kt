package com.example.eventconnect.user

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.example.eventconnect.R

class MenuActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fragmentContainer = findViewById(R.id.fragment_container)

        // Configurar el BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragment())

                    true
                }
                R.id.favorite -> {
                    loadFragment(FavoriteFragment())
                    true
                }
                R.id.perfil -> {
                    loadFragment(PerfilFragment())
                    true
                }
                R.id.settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                // Agrega más fragmentos según tus necesidades
                else -> false
            }
        }

        // Cargar el fragmento inicial
        loadFragment(HomeFragment())
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

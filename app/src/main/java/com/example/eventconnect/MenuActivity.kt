package com.example.eventconnect

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.SearchView

class MenuActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val home = Intent(this@MenuActivity, MenuActivity::class.java)
                    startActivity(home)
                    true
                }
                R.id.search -> {
                    val search = Intent(this@MenuActivity, SearchActivity::class.java)
                    startActivity(search)
                    true
                }
                R.id.favorite -> {

                    true
                }
                R.id.entradas -> {

                    true
                }
                R.id.perfil-> {

                    true
                }
                else -> false
            }
        }
    }
}


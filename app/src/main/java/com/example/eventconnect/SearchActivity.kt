package com.example.eventconnect

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val home = Intent(this@SearchActivity, MenuActivity::class.java)
                    startActivity(home)
                    true
                }
                R.id.search -> {
                    val search = Intent(this@SearchActivity, SearchActivity::class.java)
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
package com.example.eventconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var localeHelper: LocaleHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        localeHelper = LocaleHelper()

        val savedLanguage = LocaleHelper.getLanguage(this)
        savedLanguage?.let { LocaleHelper.setLocale(this, it) }

        val btnEnglish = findViewById<ImageView>(R.id.flagEnImageViewMain)
        val btnSpanish = findViewById<ImageView>(R.id.flagEsImageViewMain)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLogin = findViewById<Button>(R.id.btnLogin)


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

        btnRegister.setOnClickListener {
            val registro = Intent(this@MainActivity, RegisterActivity::class.java)
            startActivity(registro)
        }

        btnLogin.setOnClickListener {
            val login = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(login)
        }
    }
}


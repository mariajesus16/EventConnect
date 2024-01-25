package com.example.eventconnect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.Timer
import java.util.TimerTask

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tarea: TimerTask = object : TimerTask() {
            override fun run() {
                val intent = Intent(this@Splash, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        val tiempo = Timer()
        tiempo.schedule(tarea, 1000)
    }
}
package com.example.habit_tracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Maak de hele layout klikbaar
        val splashView: View = findViewById(R.id.splashImage)
        splashView.setOnClickListener {
            // Ga naar de MainActivity wanneer erop wordt geklikt
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Zorg ervoor dat de gebruiker niet terug kan naar het splash screen
        }
    }
}

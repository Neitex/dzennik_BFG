package com.neitex.dzennik_bfg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.backends.pipeline.Fresco
import io.sentry.Sentry


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fresco.initialize(this)
        val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        preferences.edit().apply()
        if (!preferences.contains("token")) {
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
        } else {
            val mainScreen = Intent(this, MainScreen::class.java)

            startActivity(mainScreen)
            finish()
        }
    }
}

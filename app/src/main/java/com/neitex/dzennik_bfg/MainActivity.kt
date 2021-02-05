package com.neitex.dzennik_bfg

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.backends.pipeline.Fresco
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        Fresco.initialize(this)
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

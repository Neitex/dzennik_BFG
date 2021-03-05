package com.neitex.dzennik_bfg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.backends.pipeline.Fresco
import com.jakewharton.threetenabp.AndroidThreeTen


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        AndroidThreeTen.init(this)
        preferences.edit().apply()
        if (!preferences.contains("token")) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            val mainScreen = Intent(this, MainScreen::class.java)
            startActivity(mainScreen)
            finish()
        }
    }
}

package com.neitex.dzennik_bfg

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View.OnKeyListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.jsonDeserializer
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.neitex.dzennik_bfg.shared_functions.hideKeyboard
import com.neitex.dzennik_bfg.shared_functions.makeSnackbar
import com.neitex.dzennik_bfg.shared_functions.saveAccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.SocketTimeoutException


class LoginScreen : AppCompatActivity() {
    var loginField: com.google.android.material.textfield.TextInputEditText? = null
    private var passwordField: com.google.android.material.textfield.TextInputEditText? = null
    var logInButton: Button? = null
    var isLoginEntered = false
    var isPasswordEntered = false
    private val schoolsAuthApi = "https://schools.by/api/auth"
    var token: String = "bruh"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initFields()
    }

    suspend fun login() {
        val authRequest = JSONObject()
        authRequest.put("username", loginField?.text)
        authRequest.put("password", passwordField?.text)


        var exceptions = 0
        var kill = false
        while (exceptions in 0..3 && !kill) {
            try {
                Fuel.post(schoolsAuthApi).jsonBody(authRequest.toString())
                    .response { _, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                if (response.statusCode == 400) {
                                    makeSnackbar(
                                        findViewById(R.id.layout_bruh),
                                        getString(R.string.wrong_auth),
                                        Color.parseColor("#B00020")
                                    )
                                    logInButton?.isEnabled = true
                                    loginField?.isActivated = true
                                    passwordField?.isActivated = true
                                    kill = true
                                    exceptions = -1337
                                } else if (result.getException() == SocketTimeoutException("SSL handshake timed out")) {
                                    makeSnackbar(
                                        findViewById(R.id.layout_bruh),
                                        getString(R.string.timeout),
                                        Color.parseColor("#B00020")
                                    )
                                    exceptions = -1337
                                }
                            }
                            is Result.Success -> {
                                token = String(result.value)
                                val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
                                val arr = JSONObject(token)
                                preferences.edit().putString("token", arr.get("token").toString())
                                    .apply()
                                makeSnackbar(
                                    findViewById(R.id.layout_bruh),
                                    getString(R.string.got_token) + ": " + arr.get("token")
                                        .toString(),
                                    resources.getColor(R.color.primaryDarkColor),
                                    Snackbar.LENGTH_SHORT
                                )
                                saveAccountInfo(
                                    arr.getString("token"),
                                    this.findViewById(R.id.layout_bruh)
                                )
                                exceptions = -2
                            }
                        }
                    }.awaitResponseResult(jsonDeserializer())
                if (kill) {
                    break
                }
                exceptions++
            } catch (e: Exception) {
                makeSnackbar(
                    findViewById(R.id.layout_bruh),
                    e.toString(),
                    Color.parseColor("#B00020")
                )
            }
        }
        if (exceptions > 3) {
            makeSnackbar(
                findViewById(R.id.layout_bruh),
                getString(R.string.unknown_error),
                Color.parseColor("#B00020")
            )
        }
        if (exceptions !in 0..3) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initFields() {
        loginField =
            findViewById(R.id.login_field)
        passwordField =
            findViewById(R.id.password_field)
        logInButton = findViewById(R.id.login_button)
        logInButton?.isEnabled = false
        loginField?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //no need
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                if (after > 30) {
                    loginField?.error = "@string/long_login_error"
                } else {
                    loginField?.error = null
                }
                isLoginEntered = after > 0
                logInButton?.isEnabled = (isLoginEntered && isPasswordEntered)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (before-count > 30) {
//                    loginField?.error = "@string/long_login_error"
//                } else {
//                    loginField?.error = null
//                }
//                isLoginEntered = before-count > 0
//                logInButton?.isEnabled = (isLoginEntered && isPasswordEntered)
            }
        })
        loginField?.setOnKeyListener(OnKeyListener { _, keyCode, event -> // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN &&
                keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                passwordField?.requestFocus()
                return@OnKeyListener true
            }
            false
        })
        passwordField?.setOnKeyListener(OnKeyListener { _, keyCode, event -> // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN &&
                keyCode == KeyEvent.KEYCODE_ENTER &&
                (isPasswordEntered && isLoginEntered)
            ) {
                passwordField?.hideKeyboard()
                GlobalScope.launch(Dispatchers.Main) {
                    login()
                }
                return@OnKeyListener true
            }
            false
        })
        passwordField?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                //no need
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                isPasswordEntered = start > 0
                logInButton?.isEnabled = (isLoginEntered && isPasswordEntered)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                isPasswordEntered = before-count > 0
//                logInButton?.isEnabled = (isLoginEntered && isPasswordEntered)
            }
        })
        logInButton?.setOnClickListener {
            it.hideKeyboard()
            logInButton?.isEnabled = false
            loginField?.isActivated = false
            passwordField?.isActivated = false
            GlobalScope.launch(Dispatchers.IO) {
                login()
            }
        }
    }

}

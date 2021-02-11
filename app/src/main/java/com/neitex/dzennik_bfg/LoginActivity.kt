package com.neitex.dzennik_bfg

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View.OnKeyListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitResponse
import com.github.kittinunf.fuel.json.jsonDeserializer
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.neitex.dzennik_bfg.shared_functions.hideKeyboard
import com.neitex.dzennik_bfg.shared_functions.makeSnackbar
import com.neitex.dzennik_bfg.shared_functions.saveAccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException
import kotlin.math.abs


class LoginActivity : AppCompatActivity() {
    private var loginField: com.google.android.material.textfield.TextInputEditText? = null
    private var passwordField: com.google.android.material.textfield.TextInputEditText? = null
    private var logInButton: Button? = null
    private var isLoginEntered = false
    private var isPasswordEntered = false
    private val schoolsAuthApi = "https://schools.by/api/auth"
    var token: String = "bruh"

    suspend fun setLoginState(state:Boolean){ //Sets login fields and buttons to input state
        withContext(Dispatchers.Main) {
            loginField?.isEnabled = state
            passwordField?.isEnabled = state
            logInButton?.isEnabled = state
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initFields()
    }

    suspend fun login() {
        val authRequest = JSONObject()
        authRequest.put("username", loginField?.text)
        authRequest.put("password", passwordField?.text)
        setLoginState(false)
        var exceptions = 0
        var startIntent = false
       loop@while (exceptions in 0..3) {
            val response = Fuel.post(schoolsAuthApi).jsonBody(authRequest.toString()).timeout(150)
                .awaitResponseResult(
                jsonDeserializer())
            when(response.third){
                is Result.Failure ->{
                    if (response.second.statusCode == 400){
                        setLoginState(true)
                        makeSnackbar(findViewById(R.id.layout_bruh),
                        resources.getString(R.string.wrong_auth))
                        break@loop
                    }
                    if (response.third.component2()?.exception is SSLException) {
                        Log.e(
                            "loginSSL",
                            "Caught SSLException at login, trying again (" +( 4 - exceptions) + " tries left)"
                        )
                        exceptions++
                        if (exceptions > 4) {
                            break@loop
                        }
                        continue@loop
                    } else if (response.third.component2()?.cause is SocketTimeoutException) {
                        if(exceptions>2) {
                            makeSnackbar(
                                findViewById(R.id.layout_bruh),
                                resources.getString(R.string.timeout)
                            )
                            break@loop
                        }
                    }
                }
                is Result.Success ->{
                    token = response.third.component1()?.obj().toString()
                    val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
                    val arr = response.third.component1()?.obj()!!
                    preferences.edit().putString("token", arr.get("token").toString())
                        .apply()
                    makeSnackbar(
                        findViewById(R.id.layout_bruh),
                        getString(R.string.got_token) + ": " + arr.get("token")
                            .toString(),
                        resources.getColor(R.color.primaryDarkColor),
                        Snackbar.LENGTH_SHORT
                    )
                    Log.d("debug", "Got token: ${arr.getString("token")}")
                    saveAccountInfo(
                        arr.getString("token"),
                        this.findViewById(R.id.layout_bruh)
                    )
                    startIntent = true
                    break@loop
                }
            }
            exceptions++
        }
        if (startIntent) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            setLoginState(true)
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
            GlobalScope.launch(Dispatchers.IO) {
                login()
            }
        }
    }

}

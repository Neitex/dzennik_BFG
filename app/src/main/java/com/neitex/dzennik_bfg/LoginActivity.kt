package com.neitex.dzennik_bfg

import android.content.Intent
import android.media.DeniedByServerException
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View.OnKeyListener
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.neitex.dzennik_bfg.shared_functions.getToken
import com.neitex.dzennik_bfg.shared_functions.hideKeyboard
import com.neitex.dzennik_bfg.shared_functions.makeSnackbar
import com.neitex.dzennik_bfg.shared_functions.saveAccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException


class LoginActivity : AppCompatActivity() {
    private var loginField: com.google.android.material.textfield.TextInputEditText? = null
    private var passwordField: com.google.android.material.textfield.TextInputEditText? = null
    private var logInButton: Button? = null
    private var isLoginEntered = false
    private var isPasswordEntered = false

    suspend fun setLoginState(state: Boolean) { //Sets login fields and buttons to input state
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
        findViewById<Button>(R.id.useTokenLoginButton).setOnClickListener {
            val intent = Intent(this, TokenLoginActivity::class.java)
            startActivity(intent)
        }
    }

    suspend fun login() {
        setLoginState(false)
        try {
            val preferences = this.getSharedPreferences("data", MODE_PRIVATE)
            val token = getToken(loginField?.text.toString(), passwordField?.text.toString())
                ?: throw IllegalArgumentException()
            saveAccountInfo(token, preferences)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: TimeoutException) {
            makeSnackbar(findViewById(R.id.login_button), getString(R.string.timeout))
            setLoginState(true)
        } catch (e: SSLException) {
            makeSnackbar(findViewById(R.id.login_button), getString(R.string.ssl_handshake_error))
            setLoginState(true)
        } catch (e: DeniedByServerException) {
            makeSnackbar(findViewById(R.id.login_button), getString(R.string.wrong_auth))
            setLoginState(true)
        } catch (e: IllegalArgumentException) {
            makeSnackbar(findViewById(R.id.login_button), getString(R.string.unknown_error))
            setLoginState(true)
        } catch (e: Exception) {
            makeSnackbar(findViewById(R.id.login_button), getString(R.string.unknown_error))
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

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

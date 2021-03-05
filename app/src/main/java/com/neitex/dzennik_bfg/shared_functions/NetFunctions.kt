package com.neitex.dzennik_bfg.shared_functions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.DeniedByServerException
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.ServerError
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.fuel.json.jsonDeserializer
import com.github.kittinunf.result.Result
import com.neitex.dzennik_bfg.R
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.abs


suspend fun getSummary(
    userID: String,
    preferences: SharedPreferences,
    date: String
): JSONObject {
    val token = preferences.all["token"]
    val apiString = "https://schools.by/subdomain-api/pupil/$userID/daybook/day/$date"
    var exceptions = 0
    loop@ while (exceptions in 0..4) {
        val response = Fuel.get(apiString).header(Headers.AUTHORIZATION, "Token $token")
            .timeout(150).awaitResponseResult(jsonDeserializer())
        when (response.third) {
            is Result.Success -> {
                return response.third.component1()?.obj()!!
            }
            is Result.Failure -> {
                if (response.third.component2()?.exception is SSLException) {
                    Log.d(
                        "summarySSL",
                        "Caught SSLException at getWeek, trying again (" + abs(4 - exceptions) + " tries left)"
                    )
                    exceptions++
                    if (exceptions > 4) {
                        throw SSLException("Something wrong with SSL in getWeek")
                    }
                    continue@loop
                } else if (response.third.component2()?.causedByInterruption == true) {
                    exceptions++
                    if (exceptions > 2)
                        throw InterruptedIOException(response.third.component2()?.cause.toString())
                } else if (response.third.component2()?.cause is SocketTimeoutException) {
                    throw TimeoutException()
                }
            }
        }
        exceptions++
    }

    return JSONObject()

}

suspend fun saveAccountInfo(token: String, preferences: SharedPreferences) {
    val userInfoApi = "https://schools.by/subdomain-api/user/current"
    var exceptions = 0
   loop@while (exceptions in 0..3) {
        val response = Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
            .awaitResponseResult(jsonDeserializer())
        when (response.third) {
            is Result.Failure -> {
                if (response.second.statusCode == 401) {
                    Sentry.captureMessage(
                        "Server rejected token in saveAccountInfo",
                        SentryLevel.WARNING
                    )
                    throw ServerError()
                }
                if (response.third.component2()?.cause is SSLHandshakeException){
                    Log.e(
                        "saveAccoutInfoSSL",
                        "Caught SSLException at saveAccoutInfo, trying again (" + abs(4 - exceptions) + " tries left)"
                    )
                    exceptions++
                    if (exceptions > 4) {
                        throw SSLException("findPupils")
                    }
                    continue@loop
                }
                if (response.third.component2()?.cause is InterruptedIOException){
                    exceptions++
                    if (exceptions > 4) {
                        throw InterruptedIOException()
                    }
                    continue@loop
                }
            }
            is Result.Success -> {
                val data = response.third.component1()?.obj()!!
                preferences.edit()
                    .putString("user_type", data.get("type").toString()).apply()
                preferences.edit()
                    .putString("first_name", data.get("first_name").toString())
                    .putString("last_name", data.get("last_name").toString())
                    .apply()
                preferences.edit()
                    .putString("id", data.get("id").toString()).apply()
                preferences.edit().putString("token", token).apply()
                return
            }
        }
        exceptions++
    }
    throw Exception("Unknown exception occured")
}

suspend fun findPupils(token: String, userID: String, view: View): JSONArray? {
    val userInfoApi = "https://schools.by/subdomain-api/parent/$userID/pupils"
    var exceptions = 0
    var pupils: JSONArray? = JSONArray()
    var kill = false
    try {
        loop@ while (exceptions in 0..4 && !kill) {
            var response: Triple<Request, Response, Result<FuelJson, FuelError>>
            withContext(Dispatchers.IO) {
                response =
                    Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
                        .timeout(150).awaitResponseResult(jsonDeserializer())
            }
            when (response.third) {
                is Result.Success -> {
                    Log.d("DEV", response.third.component1()?.array().toString())
                    return response.third.component1()?.array()
                }
                is Result.Failure -> {
                    if (response.third.component2()?.exception is SSLHandshakeException) {
                        Log.d(
                            "findPupils",
                            "Caught SSLException at findPupils, " + abs(4 - exceptions).toString() + " tries left"
                        )
                        exceptions++
                        if (exceptions > 4) {
                            throw SSLException("findPupils")
                        }
                        continue@loop
                    } else if (response.third.component2()?.causedByInterruption == true) {
                        Log.d("DEV", "Caught interruption at findPupils")
                        if (exceptions > 2) {
                            throw InterruptedIOException()
                        }
                    } else if (response.third.component2()?.exception is SocketTimeoutException) {
                        Log.d("DEV", "Caught timeout at findPupils")
                        if (exceptions > 2)
                            throw TimeoutException()
                    }
                    Log.d("DEV", response.third.component2()?.exception.toString())
                }
            }
            exceptions++
        }
    } catch (e: Exception) {
        makeSnackbar(
            view,
            e.toString(),
            Color.parseColor("#B00020")
        )
    }
    return pupils

}

suspend fun getWeek(
    userID: String,
    token: String,
    date: String
): JSONObject {
    val apiString = "https://schools.by/subdomain-api/pupil/$userID/daybook/week/$date"
    var exceptions = 0
    loop@ while (exceptions in 0..4) {
        val response = Fuel.get(apiString).header(Headers.AUTHORIZATION, "Token $token")
            .timeout(150).awaitResponseResult(jsonDeserializer())
        when (response.third) {
            is Result.Success -> {
                return response.third.component1()?.obj()!!
            }
            is Result.Failure -> {
                if (response.third.component2()?.exception is SSLException) {
                    Log.d(
                        "summarySSL",
                        "Caught SSLException at getWeek, trying again (" + abs(4 - exceptions) + " tries left)"
                    )
                    exceptions++
                    if (exceptions > 4) {
                        throw SSLException("Something wrong with SSL in getWeek")
                    }
                    continue@loop
                } else if (response.third.component2()?.causedByInterruption == true) {
                    exceptions++
                    if (exceptions > 2)
                        throw InterruptedIOException(response.third.component2()?.cause.toString())
                } else if (response.third.component2()?.cause is SocketTimeoutException) {
                    throw TimeoutException()
                }
            }
        }
        exceptions++
    }
    return JSONObject() //basically unreachable
}

suspend fun getUserInfo(userID: String, token: String): JSONObject {
    val apiString = "https://schools.by/subdomain-api/pupil/$userID/info"
    var exceptions = 0
    loop@ while (exceptions in 0..4) {
        val response = Fuel.get(apiString).header(Headers.AUTHORIZATION, "Token $token")
            .timeout(150).awaitResponseResult(jsonDeserializer())
        when (response.third) {
            is Result.Success -> {
                return response.third.component1()?.obj()!!
            }
            is Result.Failure -> {
                if (response.third.component2()?.exception is SSLException) {
                    Log.d(
                        "summarySSL",
                        "Caught SSLException at getUserInfo, trying again (" + abs(4 - exceptions) + " tries left)"
                    )
                    exceptions++
                    if (exceptions > 4) {
                        throw SSLException("Something wrong with SSL in getUserInfo")
                    }
                    continue@loop
                } else if (response.third.component2()?.causedByInterruption == true) {
                    exceptions++
                    if (exceptions > 2)
                        throw InterruptedIOException(response.third.component2()?.cause.toString())
                } else if (response.third.component2()?.cause is SocketTimeoutException) {
                    throw TimeoutException()
                }
            }
        }
        exceptions++
    }
    return JSONObject() //basically unreachable
}

suspend fun isValidToken(token: String): Boolean {
    var isValid = false
    val userInfoApi = "https://schools.by/subdomain-api/user/current"
    var exceptions = 0
    try {
        while (exceptions in 0..3) {
            val response = Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
                .awaitResponseResult(jsonDeserializer())
            when (response.third) {
                is Result.Success -> {
                    return true
                }
                is Result.Failure -> {
                    if (response.second.statusCode == 401) {
                        return false
                    }
                }
            }
            exceptions++
        }
    } catch (e: Exception) {
        //TODO: Rewrite this piece of (bad) code
    }
    return isValid
}

suspend fun getToken(username: String, password: String):String?{
    val authRequest = JSONObject()
    authRequest.put("username",username)
    authRequest.put("password", password)
    val schoolsAuthApi = "https://schools.by/api/auth"
    var exceptions = 0
    loop@ while (exceptions in 0..4) {
        val response = Fuel.post(schoolsAuthApi).jsonBody(authRequest.toString()).timeout(150)
            .awaitResponseResult(
                jsonDeserializer()
            )
        when (response.third) {
            is Result.Failure -> {
                if (response.second.statusCode == 400) {
                 throw DeniedByServerException("Authentication failed")
                }
                if (response.third.component2()?.exception is SSLException) {
                    Log.e(
                        "loginSSL",
                        "Caught SSLException at login, trying again (" + (4 - exceptions) + " tries left)"
                    )
                    exceptions++
                    if (exceptions > 5) {
                       throw SSLException("SSL Exception in login")
                    }
                    continue@loop
                } else if (response.third.component2()?.cause is SocketTimeoutException) {
                    if (exceptions > 3) {
                        throw TimeoutException()
                    }
                }
            }
            is Result.Success -> {
                return response.third.component1()?.obj()?.get("token").toString()
                break@loop
            }
        }
        exceptions++
    }
    return null
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!
        .isConnected
}


fun isInternetAvailable(): Boolean {
    try {
        val address: InetAddress = InetAddress.getByName("www.example.com")
        return !address.equals("")
    } catch (e: UnknownHostException) {
    }
    return false
}
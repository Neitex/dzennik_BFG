package com.neitex.dzennik_bfg.shared_functions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
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
                Log.d("dev", response.third.component1()?.obj().toString())
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
        Log.d("dev", "Request failed. Cause: " + response.third.component2()?.exception.toString())
        exceptions++
    }

    return JSONObject()

}

fun saveAccountInfo(token: String, view: View) {
    val userInfoApi = "https://schools.by/subdomain-api/user/current"
    var exceptions = 0
    try {
        while (exceptions in 0..3) {
            Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
                .responseString { _, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            if (response.statusCode == 401) {
                                makeSnackbar(
                                    view.findViewById(R.id.layout_bruh),
                                    view.resources.getString(R.string.token_invalid_login)
                                )
                                exceptions = -2
                            }
                            Sentry.captureMessage(
                                "Server rejected token in saveAccountInfo",
                                SentryLevel.WARNING
                            )
                        }
                        is Result.Success -> {
                            val data = JSONObject(String(response.data))
                            val preferences = view.context.getSharedPreferences(
                                "data",
                                AppCompatActivity.MODE_PRIVATE
                            )
                            preferences.edit()
                                .putString("user_type", data.get("type").toString()).apply()
                            preferences.edit()
                                .putString("first_name", data.get("first_name").toString())
                                .putString("last_name", data.get("last_name").toString())
                                .apply()
                            preferences.edit()
                                .putString("id", data.get("id").toString()).apply()
                            exceptions = -11
                        }
                    }

                }.join()
            exceptions++
        }
    } catch (e: Exception) {
        makeSnackbar(
            view,
            e.toString(),
            Color.parseColor("#B00020")
        )
    }
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
                    Log.wtf("dev", response.third.component2()?.exception.toString())
                    Log.wtf("dev", response.first.url.toString())
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
        Log.d("deb", e.toString())
    }
    return pupils

}

suspend fun getWeek(
    userID: String,
    preferences: SharedPreferences,
    date: String
): JSONObject {
    val token = preferences.all["token"]
    val apiString = "https://schools.by/subdomain-api/pupil/$userID/daybook/week/$date"
    var exceptions = 0
    loop@ while (exceptions in 0..4) {
        val response = Fuel.get(apiString).header(Headers.AUTHORIZATION, "Token $token")
            .timeout(150).awaitResponseResult(jsonDeserializer())
        when (response.third) {
            is Result.Success -> {
                Log.d("dev", response.third.component1()?.obj().toString())
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
        Log.d("dev", response.third.component2()?.cause.toString())
        Log.wtf("dev", response.first.url.toString())
        Log.d("dev", response.third.component1()?.obj().toString())
        exceptions++
    }
    return JSONObject() //basically unreachable
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
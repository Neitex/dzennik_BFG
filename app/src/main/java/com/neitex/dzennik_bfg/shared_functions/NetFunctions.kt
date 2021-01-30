package com.neitex.dzennik_bfg.shared_functions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.json.jsonDeserializer
import com.github.kittinunf.result.Result
import com.neitex.dzennik_bfg.R
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException


suspend fun GetSummary(
    userID: String,
    preferences: SharedPreferences,
    date: String,
    view: View
): JSONObject? {
    val token = preferences.all["token"]
    val apiString = "https://schools.by/subdomain-api/pupil/$userID/daybook/day/$date"
    var exceptions = 0
    var kill = false
    var summary: JSONObject? = null
    try {
        while (exceptions in 0..1337 && !kill) {
            val response = Fuel.get(apiString).header(Headers.AUTHORIZATION, "Token $token")
                .timeout(150).awaitObjectResponseResult(jsonDeserializer())
            if (response.third.component2() == null) {
                if (response.second.statusCode == 401) {
                    makeSnackbar(
                        view,
                        view.context.resources.getString(R.string.token_invalid)
                    )
                    summary = null
                    break
                } else if (response.second.statusCode == 200) {
                    kill = true
                    summary = response.third.component1()?.obj()
                    break
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
        Sentry.captureMessage("Unknown exception: $e", SentryLevel.ERROR)
    }
    if (!kill) {
        makeSnackbar(
            view,
            view.context.resources.getString(R.string.unknown_error)
        )

    }
    return summary

}

fun saveAccountInfo(token: String, view: View) {
    val userInfoApi = "https://schools.by/subdomain-api/user/current"
    var Exceptions = 0
    try {
        while (Exceptions in 0..3) {
            Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            if (response.statusCode == 401) {
                                makeSnackbar(
                                    view.findViewById(R.id.layout_bruh),
                                    view.resources.getString(R.string.token_invalid_login)
                                )
                                Exceptions = -2
                            }
                            Sentry.captureMessage("Server rejected token in saveAccountInfo", SentryLevel.WARNING)
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
                            Exceptions = -11
                        }
                    }

                }.join()
            Exceptions++
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
    var kill: Boolean = false

    try {
        while (exceptions in 0..1337 && !kill) {
            val response = Fuel.get(userInfoApi).header(Headers.AUTHORIZATION, "Token $token")
                .timeout(150).awaitObjectResponseResult(jsonDeserializer())
            if (response.third.component2() == null) {
                if (response.second.statusCode == 401) {
                    makeSnackbar(
                        view,
                        view.context.resources.getString(R.string.token_invalid)
                    )
                    pupils = null
                    break
                } else if (response.second.statusCode == 200) {
                    kill = true
                    pupils = response.third.component1()?.array()
                    break
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
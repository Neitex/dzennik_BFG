package com.neitex.dzennik_bfg

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.neitex.dzennik_bfg.fragments.*
import com.neitex.dzennik_bfg.shared_functions.*
import io.sentry.Sentry
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.TemporalAdjusters
import java.io.InterruptedIOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException


private lateinit var preferences: SharedPreferences
private lateinit var view: View
private var pupilsArray: JSONArray? = null
private var pupilID = -1
private var pupilIDString: String = ""
private lateinit var fileResources: Resources
private lateinit var myFragmentManager: FragmentManager
private lateinit var myLifecycle: Lifecycle


class MainScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Fresco.initialize(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        val viewPager = findViewById<ViewPager2>(R.id.page_view)
        view = viewPager.rootView
        fileResources = resources
        myFragmentManager = supportFragmentManager
        myLifecycle = lifecycle
        val tabs = findViewById<TabLayout>(R.id.menuTabs)
        supportFragmentManager
        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, null)
        viewPager.offscreenPageLimit = 4
        TabLayoutMediator(tabs, viewPager)
        { _, position ->
            viewPager.currentItem = position
        }.attach()
        val tabsNames = arrayOf(
            getString(R.string.main_page_name),
            getString(R.string.diary_name),
            getString(R.string.settings_tab_name)
        )
        for (i in 0..tabs.tabCount) {
            tabs.getTabAt(i)?.text = tabsNames[i]
        }
        tabs.selectTab(tabs.getTabAt(0))
        getBasicData().start()
        viewPager.isUserInputEnabled = false
    }

    class ViewStateAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val weekData: JSONObject?
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> {
                    if (weekData != null) {
                        val calendar = Calendar.getInstance()
                        var isTommorow = true
                        try {
                            val currentDay = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(calendar.time)
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
                                isTommorow = false
                                calendar.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            val upcomingDay = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(calendar.time)
                            var summPage = SummaryPage()
                            val bundle = Bundle()

                            bundle.putString(
                                "currentDay",
                                weekData.getJSONObject(currentDay).toString()
                            )
                            bundle.putString(
                                "upcomingDay",
                                weekData.getJSONObject(upcomingDay).toString()
                            )
                            bundle.putBoolean("isTomorrow", isTommorow)
                            summPage.arguments = bundle
                            return summPage
                        } catch (e: JSONException) {
                            Sentry.addBreadcrumb("JSON", weekData.toString())
                            Sentry.captureException(e)
                            return ErrorFragment(e)
                        }
                    } else return Fragment()
                }
                1 -> {
                    if (weekData != null) {
                        val diary = DiaryPage()
                        val calendar = Calendar.getInstance()
                        val mondayDate = org.threeten.bp.LocalDate.ofYearDay(
                            calendar[Calendar.YEAR],
                            calendar[Calendar.DAY_OF_YEAR]
                        ).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                        var userID = ""
                        if (preferences.all["user_type"] == "Parent") {
                            userID = pupilsArray?.getJSONObject(pupilID)?.getInt("id").toString()
                        } else {
                            userID = preferences.all["id"].toString()
                        }
                        val bundle = Bundle()
                        bundle.putString("userID", userID)
                        bundle.putString("currWeek", Pair(mondayDate, weekData).toString())
                        diary.arguments = bundle
                        return diary
                    } else return ErrorFragment(null)
                }
                2 -> {
                    return ErrorFragment(null)
                }
                else -> {
                    throw RuntimeException("$position is outside of tabs range")
                }
            }
        }

        override fun getItemCount(): Int {
            return 3
        }
    }


    private fun getBasicData() = GlobalScope.async(Dispatchers.Main) {
        try {
            findViewById<ProgressBar>(R.id.progressBar2).visibility = ProgressBar.VISIBLE
            try {
                if (preferences.all["user_type"] == "Parent") {
                    val pupils =
                        findPupils(
                            preferences.all["token"].toString(),
                            preferences.all["id"].toString(),
                            view
                        )
                    Log.d("Got pupils", pupils.toString())
                    if (pupils == null) {
                        Log.d("cum","cum cum")
                        this@async.cancel()
                    } else {
                        pupilsArray = filterPupils(pupils, preferences)
                        Log.d("Filtered", "cum" + pupilsArray.toString())
                    }

                } else if (preferences.all["user_type"] == "Pupil") {
                    pupilIDString = preferences.all["id"].toString()
                }
                when (preferences.all["user_type"]) {
                    "Parent" -> {
                        changePupil(0)
                    }
                    "Pupil" -> {
                        withContext(Dispatchers.IO) {
                            updatePages(preferences.all["id"].toString())
                        }
                    }
                    else -> {
                        Sentry.captureException(IllegalArgumentException("Unknown user type"))
                    }
                }
            } catch (e: SSLException) {
                Log.d("ssl", "cum")
                //TODO: Switch to offline mode
            } catch (e: TimeoutException) {
                Log.d("tmt", "cum")
                //TODO: Switch to offline mode
            } catch (e: InterruptedIOException) {
                Log.d("iio", "cum")
                //TODO: Switch to offline mode
            }
        } catch (e: Exception) {
        } finally {
            findViewById<ProgressBar>(R.id.progressBar2).visibility = ProgressBar.GONE
        }
    }

}

fun getName(id: Int): String =
    pupilsArray?.getJSONObject(id)?.getString("last_name") + ' ' + pupilsArray?.getJSONObject(id)
        ?.getString("first_name")

fun getUserID(id: Int): String {
    if (id == -1) {
        return preferences.all["id"].toString()
    } else {
        return pupilsArray?.getJSONObject(id)?.getString("id").toString()
    }
}

suspend fun getAvatar(): String? {
    val id = pupilID
    if (pupilsArray == null) {
        val userInfo = getUserInfo(getUserID(id), preferences.all["token"].toString())
        try {
            val str = userInfo.get("photo")
                .toString()
            if (str == null.toString()) {
                return null
            } else {
                return str
            }
        } catch (e: InterruptedIOException) {
            return null
        } catch (e: TimeoutException) {
            return null
        } catch (e: JSONException) {
            Sentry.addBreadcrumb("No value for photo", userInfo.toString())
            Sentry.captureException(e)
            return null
        }
    } else {
        val str = pupilsArray?.getJSONObject(id)?.get("photo").toString()
        if (str == null.toString()) {
            return null
        } else {
            return str
        }
    }
}

fun updateNameText(textView: TextView) {
    if (preferences.all["user_type"] == "Pupil") {
        textView.text =
            preferences.all["last_name"].toString() + ' ' + preferences.all["first_name"].toString()
    } else {
        textView.text = getName(pupilID)
        if (pupilsArray != null) {
            if (pupilsArray!!.length() > 1) {
                textView.setOnClickListener {
                    val pupilsDialog = ChangePupilsDialog(pupilsArray!!)
                    pupilsDialog.show(myFragmentManager, pupilsDialog.tag)
                }
            }
        }
    }
    TextViewCompat.setAutoSizeTextTypeWithDefaults(
        textView,
        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
    )
}

suspend fun updatePages(userID: String) {
    var weekSummary = JSONObject()
    try {
        val calendar = Calendar.getInstance()
        val currDayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
        val currentWeekMonday = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(calendar.time)
        weekSummary = getWeek(userID, preferences.all["token"].toString(), currentWeekMonday)
        if (currDayOfWeek == Calendar.SUNDAY || currDayOfWeek == Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val mondayDayString = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(calendar.time)
            weekSummary.accumulate(
                mondayDayString,
                getSummary(userID, preferences, mondayDayString)
            )
        }
        withContext(Dispatchers.Main) {
            val pageView = view.findViewById<ViewPager2>(R.id.page_view)
            pageView.adapter =
                MainScreen.ViewStateAdapter(myFragmentManager, myLifecycle, weekSummary)
        }
    } catch (e: TimeoutException) {
        //TODO: Switch to offline mode
        makeSnackbar(view, fileResources.getString(R.string.timeout))
    } catch (e: SSLException) {
        //TODO: Switch to offline mode
        makeSnackbar(view, fileResources.getString(R.string.ssl_handshake_error))
    }
}


fun changePupil(newPupil: Int) {
    val pupilsArrayNow = pupilsArray
    if (pupilsArrayNow != null) {
        if (newPupil >= pupilsArrayNow.length()) {
            throw IllegalArgumentException("$newPupil (newPupil) is bigger than pupilsArrayNow size (${pupilsArrayNow.length()})")
        }

        if (newPupil != pupilID) {
            pupilID = newPupil
            GlobalScope.launch(Dispatchers.IO) {
                updatePages(getUserID(newPupil))
            }
        }
    }
}
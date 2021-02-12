package com.neitex.dzennik_bfg

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jakewharton.threetenabp.AndroidThreeTen
import com.neitex.dzennik_bfg.fragments.ChangePupilsDialog
import com.neitex.dzennik_bfg.fragments.DiaryPage
import com.neitex.dzennik_bfg.fragments.NotImplementedPage
import com.neitex.dzennik_bfg.fragments.SummaryPage
import com.neitex.dzennik_bfg.shared_functions.findPupils
import com.neitex.dzennik_bfg.shared_functions.getSummary
import com.neitex.dzennik_bfg.shared_functions.getWeek
import com.neitex.dzennik_bfg.shared_functions.makeSnackbar
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.Temporal
import org.threeten.bp.temporal.TemporalAdjuster
import org.threeten.bp.temporal.TemporalAdjusters
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException


private lateinit var preferences: SharedPreferences
private lateinit var view: View
private lateinit var pupilsArray: JSONArray
private var pupilID: String = ""
private lateinit var fileResources: Resources
private lateinit var myFragmentManager: FragmentManager
private lateinit var myLifecycle: Lifecycle


class MainScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        val viewPager = findViewById<ViewPager2>(R.id.page_view)
        view = viewPager.rootView
        fileResources = resources
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            findViewById(R.id.pupilName),
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        myFragmentManager = supportFragmentManager
        myLifecycle = lifecycle
        val tabs = findViewById<TabLayout>(R.id.menuTabs)
        supportFragmentManager
        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, null)

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
                        summPage.initFields(
                            preferences, weekData.getJSONObject(currentDay),
                            weekData.getJSONObject(upcomingDay),
                            isTommorow
                        )
                        return summPage
                    } else return Fragment()
                }
                1 -> {
                    if (weekData!=null) {
                        val diary = DiaryPage()
                        val calendar = Calendar.getInstance()
                        val mondayDate = org.threeten.bp.LocalDate.ofYearDay(
                            calendar[Calendar.YEAR],
                            calendar[Calendar.DAY_OF_YEAR]
                        ).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                        diary.initFields(pupilID, Pair(mondayDate, weekData), preferences)
                        return diary
                    } else return NotImplementedPage()
                }
                2 -> {
                    return NotImplementedPage()
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
                    if (pupils == null) {
                        this@async.cancel()
                    } else
                        pupilsArray = pupils

                } else if (preferences.all["user_type"] == "Pupil") {
                    pupilID = preferences.all["id"].toString()
                }
                if (preferences.all["user_type"] == "Parent") {
                    changePupil(0)
                } else if (preferences.all["user_type"] == "Pupil") {
                    updateName()
                }
            } catch (e: SSLException) {
                //TODO: Switch to offline mode
            } catch (e: TimeoutException) {
                //TODO: Switch to offline mode
            } catch (e: InterruptedException) {
                //TODO: Switch to offline mode
            }
        } catch (e: Exception) {
        } finally {
            findViewById<ProgressBar>(R.id.progressBar2).visibility = ProgressBar.GONE
        }
    }

    fun updateName() {
        view.findViewById<TextView>(R.id.pupilName).text =
            preferences.all["last_name"].toString() + ' ' + preferences.all["first_name"].toString()
        view.findViewById<ViewPager2>(R.id.page_view).adapter = null
        view.findViewById<ViewPager2>(R.id.page_view).adapter =
            ViewStateAdapter(myFragmentManager, myLifecycle, null)
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            view.findViewById(R.id.pupilName),
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        GlobalScope.launch(Dispatchers.IO) {
            updatePages(preferences.all["id"].toString())
        }
    }

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
        weekSummary = getWeek(userID, preferences, currentWeekMonday)
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
            (pageView.adapter as MainScreen.ViewStateAdapter).notifyDataSetChanged()
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
    if (newPupil >= pupilsArray.length()) {
        throw IllegalArgumentException("$newPupil (newPupil) is bigger than pupilsArray size (${pupilsArray.length()})")
    }

    if (pupilsArray.getJSONObject(newPupil).getInt("id").toString() != pupilID) {
        if (pupilsArray.length() != 1) {
            view.findViewById<TextView>(R.id.pupilName).setOnClickListener {
                val pupilsDialog = ChangePupilsDialog(pupilsArray)
                pupilsDialog.show(myFragmentManager, pupilsDialog.tag)
            }
        }
        pupilID = pupilsArray.getJSONObject(newPupil).getInt("id").toString()
        view.findViewById<TextView>(R.id.pupilName).text =
            pupilsArray.getJSONObject(newPupil)?.getString("last_name") + ' ' +
                    pupilsArray.getJSONObject(newPupil)?.getString("first_name")
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            view.findViewById(R.id.pupilName),
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        GlobalScope.launch(Dispatchers.IO) {
            updatePages(pupilID)
        }
    }
}
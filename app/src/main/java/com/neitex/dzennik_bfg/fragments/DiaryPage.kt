package com.neitex.dzennik_bfg.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.fragments.diary_fragments.WeekSummaryMainPage
import io.sentry.Sentry
import org.json.JSONObject
import java.util.*

class DiaryPage : Fragment() {
    var userID: String = null.toString()
    private lateinit var currentWeek: Pair<String, JSONObject>
    private lateinit var preferences: SharedPreferences
    fun initFields(
        usrID: String,
        currWeek: Pair<String, JSONObject>,
        prefs: SharedPreferences
    ) {
        userID = usrID
        currentWeek = currWeek
        preferences = prefs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (userID == null.toString()) {
            Sentry.captureMessage("Created TimetableView without initializing userID")
            throw Exception("Created TimetableView without initializing userID")
        }
        var calendar = Calendar.getInstance()
        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY)
            calendar.roll(Calendar.DAY_OF_YEAR, -1)
        calendar.roll(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
        val view = inflater.inflate(R.layout.diary_page_fragment, container, false)
        val mainTabs = view.findViewById<TabLayout>(R.id.tabLayout)
        val diaryPageViewPager = view.findViewById<ViewPager2>(R.id.diaryPageViewPager)
        diaryPageViewPager.adapter =
            ViewStateAdapter(childFragmentManager, lifecycle, currentWeek, userID, preferences)
        TabLayoutMediator(mainTabs, diaryPageViewPager)
        { _, position ->
            diaryPageViewPager.currentItem = position
        }.attach()
        val tabsNames = arrayOf(
            getString(R.string.timetable_name),
            getString(R.string.last_page_name)
        )
        for (i in 0..mainTabs.tabCount) {
            mainTabs.getTabAt(i)?.text = tabsNames[i]
        }
        mainTabs.selectTab(mainTabs.getTabAt(0))
        return view
    }

    class ViewStateAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        val currWeek: Pair<String, JSONObject>,
        val userID: String,
        val prefs: SharedPreferences
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> {
                    val weekPage = WeekSummaryMainPage()
                    weekPage.initFields(currWeek.second, currWeek.first, userID, prefs)
                    return weekPage
                }
                1 -> {
                    return NotImplementedPage()
                }
                else -> {
                    throw RuntimeException("$position is outside of tabs range")
                }
            }
        }

        override fun getItemCount(): Int {
            return 2
        }
    }
}
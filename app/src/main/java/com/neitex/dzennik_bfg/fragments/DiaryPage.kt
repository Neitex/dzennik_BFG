package com.neitex.dzennik_bfg.fragments

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.fragments.diary_fragments.WeekSummaryMainPage
import com.neitex.dzennik_bfg.shared_functions.SingleDirectionViewPager
import io.sentry.Sentry
import org.json.JSONObject
import java.lang.Exception
import java.util.*

class DiaryPage : Fragment() {
    var userID: String = null.toString()
    private lateinit var currentWeek: Pair<String, JSONObject>
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = arguments
        if(args == null){
            Sentry.captureMessage("Created DiaryPage without bundle")
            throw Exception("Created DiaryPage without bundle")
        }
        userID = args.getString("userID").toString()
        val currentWeekFirst = args.getString("currWeek")?.drop(1)?.replaceAfter(',', "")?.dropLast(1)
        val currentWeekSecond = args.getString("currWeek")?.replaceBefore('{',"")?.dropLast(1)
        currentWeek = Pair(currentWeekFirst!!, JSONObject(currentWeekSecond))
        var calendar = Calendar.getInstance()
        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY)
            calendar.roll(Calendar.DAY_OF_YEAR, -1)
        calendar.roll(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
        val view = inflater.inflate(R.layout.diary_page_fragment, container, false)
        val mainTabs = view.findViewById<TabLayout>(R.id.tabLayout)
        val diaryPageViewPager = view.findViewById<ViewPager2>(R.id.diaryPageViewPager)
        diaryPageViewPager.adapter =
            context?.getSharedPreferences("data", MODE_PRIVATE)?.getString("token", "")?.let {
                ViewStateAdapter(childFragmentManager, lifecycle, currentWeek, userID,
                    it
                )
            }
        diaryPageViewPager.isUserInputEnabled = false
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
        preferences = context?.getSharedPreferences("data", MODE_PRIVATE)!!
        return view
    }

    class ViewStateAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        val currWeek: Pair<String, JSONObject>,
        val userID: String,
        val token:String
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> {
                    val weekPage = WeekSummaryMainPage()
                    val bundle = Bundle()
                    bundle.putString("currWeek", currWeek.toString())
                    bundle.putString("userID", userID)
                    bundle.putString("token", token)
                    weekPage.arguments = bundle
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
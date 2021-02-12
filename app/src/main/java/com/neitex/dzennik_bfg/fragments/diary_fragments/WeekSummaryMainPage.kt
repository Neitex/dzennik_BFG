package com.neitex.dzennik_bfg.fragments.diary_fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.SwipeDirection
import com.neitex.dzennik_bfg.shared_functions.WeekPageAdapter
import org.json.JSONObject

class WeekSummaryMainPage : Fragment() {
    private var userID: String = null.toString()
    private lateinit var preferences: SharedPreferences
    val weekData: MutableLiveData<MutableMap<String, JSONObject>> by lazy { //TODO: Добавить поле позиции
        MutableLiveData<MutableMap<String, JSONObject>>()
    }

    public fun initFields(
        currentWeek: JSONObject,
        currentWeekMonday: String,
        usrID: String,
        prefs: SharedPreferences
    ) {
        weekData.value = mutableMapOf()
        weekData.value?.put(currentWeekMonday, currentWeek)
        userID = usrID
        preferences = prefs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.week_main_fragment, container, true)
        val viewPager =
            view.findViewById<ViewPager>(R.id.week_fragment)
//        viewPager.setAllowedSwipeDirection(SwipeDirection.NONE)
        viewPager.adapter = WeekPageAdapter(childFragmentManager, weekData)
        viewPager.setCurrentItem(Int.MAX_VALUE/2)
        return view
    }
}
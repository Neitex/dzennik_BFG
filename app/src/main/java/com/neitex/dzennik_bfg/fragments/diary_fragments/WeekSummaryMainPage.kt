package com.neitex.dzennik_bfg.fragments.diary_fragments

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.SingleDirectionViewPager
import com.neitex.dzennik_bfg.shared_functions.SwipeDirection
import com.neitex.dzennik_bfg.shared_functions.WeekPageAdapter
import com.neitex.dzennik_bfg.shared_functions.getWeek
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class WeekSummaryMainPage : Fragment() {
    private var userID: String = null.toString()
    private lateinit var token: String
    private lateinit var viewModel: WeekViewData

    class WeekViewData : ViewModel() {
        private val weekLiveData: MutableLiveData<MutableMap<Int, JSONObject>> by lazy {
            MutableLiveData<MutableMap<Int, JSONObject>>().also {
                if (it.value == null)
                    it.value = mutableMapOf()
            }
        }
        val weeks: LiveData<MutableMap<Int, JSONObject>>
            get() = weekLiveData

        fun pushData(id: Int, data: JSONObject) {
            val yeet = weekLiveData.value
            yeet?.put(id, data)
            weekLiveData.value = yeet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this.requireActivity()).get(WeekViewData::class.java)
        val args = arguments
        if (args == null) {
            Sentry.captureMessage("Created WeekSummaryMainPage without bundle")
            throw Exception("Created WeekSummaryMainPage without bundle")
        }
        userID = args.getString("userID").toString()
        token = args.getString("token").toString()
        val currentWeekFirst = args.getString("currWeek")?.drop(1)?.replaceAfter(',', "")?.dropLast(
            1
        )
        val currentWeekSecond = args.getString("currWeek")?.replaceBefore('{', "")?.dropLast(1)
        viewModel
        if (currentWeekFirst != null) {
            viewModel.pushData(0, JSONObject(currentWeekSecond))
        }
        //Download previous and next week data
        GlobalScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            var weekData = getWeek(
                userID,
                token,
                SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            )
            withContext(Dispatchers.Main) {
                viewModel.pushData(-1, weekData)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 14)
            weekData = getWeek(
                userID,
                args.getString("token", ""),
                SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            )
            withContext(Dispatchers.Main) {
                viewModel.pushData(1, weekData)
            }
        }


        val view = inflater.inflate(R.layout.week_main_fragment, container, false)
        val viewPager =
            view.findViewById<SingleDirectionViewPager>(R.id.week_fragment)
        viewPager.setAllowedSwipeDirection(SwipeDirection.ALL)
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = WeekPageAdapter(
            parentFragmentManager,
            view.context.getSharedPreferences("data", MODE_PRIVATE)
                .getBoolean("isMarksAllowed", true)
        )
        viewPager.currentItem = Int.MAX_VALUE / 2
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                val currPosition = position - (Int.MAX_VALUE / 2)
                var nextPageAdder: Int = when {
                    currPosition < 0 -> -1
                    currPosition > 0 -> 1
                    else -> 0
                }
                if (viewModel.weeks.value?.contains(currPosition + nextPageAdder) == false) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val calendar = Calendar.getInstance()
                        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
                            calendar.add(Calendar.DAY_OF_YEAR, -1)
                        }
                        calendar.add(
                            Calendar.DAY_OF_YEAR,
                            Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK]
                        )
                        calendar.add(Calendar.DAY_OF_YEAR, currPosition * 7)
                        val week = getWeek(
                            userID,
                            token,
                            SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.pushData(currPosition, week)
                        }
                    }
                }
            }
        })

        viewModel.weeks.observe(viewLifecycleOwner, androidx.lifecycle.Observer { item ->
            if ((Int.MAX_VALUE / 2) - viewPager.currentItem < 0) {
                if (item.containsKey((Int.MAX_VALUE / 2) - viewPager.currentItem - 1)) {
                    viewPager.setAllowedSwipeDirection(SwipeDirection.ALL)
                } else {
                    viewPager.setAllowedSwipeDirection(SwipeDirection.RIGHT)
                }
            } else if ((Int.MAX_VALUE/2)-viewPager.currentItem>0){
                if (item.containsKey((Int.MAX_VALUE / 2) - viewPager.currentItem + 1)) {
                    viewPager.setAllowedSwipeDirection(SwipeDirection.ALL)
                } else {
                    viewPager.setAllowedSwipeDirection(SwipeDirection.LEFT)
                }
            } else {
                viewPager.setAllowedSwipeDirection(SwipeDirection.ALL)
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewPager =
            view.findViewById<SingleDirectionViewPager>(R.id.week_fragment)
        viewPager.setAllowedSwipeDirection(SwipeDirection.ALL)
    }


}
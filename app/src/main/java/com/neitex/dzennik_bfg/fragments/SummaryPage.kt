package com.neitex.dzennik_bfg.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class SummaryPage(
    var userID: String,
    var preferences: SharedPreferences,
    var viewer: View,
    val position: Int = -1
) :
    Fragment() {
    var currentDaySummary: JSONObject? = null
    var upcomingDaySummary: JSONObject? = null
    var isTommorow: Boolean = true
    var defaultToolbarSize : Int = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        runBlocking(Dispatchers.IO) {
            val currentDay = Calendar.getInstance()
            val todayDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDay.time)
            currentDay.set(Calendar.DAY_OF_YEAR, currentDay[Calendar.DAY_OF_YEAR] + 1)
            Log.d("ummmm", currentDay[Calendar.DAY_OF_WEEK].toString())
            if (currentDay[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
                currentDay.set(Calendar.DAY_OF_YEAR, currentDay[Calendar.DAY_OF_YEAR] + 1)
                isTommorow = false
            }
            val nextDate =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDay.time)
            currentDaySummary = GetSummary(userID, preferences, todayDate, viewer)
            upcomingDaySummary = GetSummary(userID, preferences, nextDate, viewer)

            defaultToolbarSize = viewer.findViewById<AppBarLayout>(R.id.appbar).layoutParams.height
        }
        return inflater.inflate(
            R.layout.summary_fragment,
            container,
            false
        ) //в душе не ебу, что это. я просто скопировал это с d.android.com
    }

    override fun onResume() {
        super.onResume()
        if (position == 2) {
            viewer.findViewById<AppBarLayout>(R.id.appbar).setExpanded(false, true)
            viewer.findViewById<AppBarLayout>(R.id.appbar).setActivated(false)
            viewer.findViewById<AppBarLayout>(R.id.appbar).getLayoutParams().height = 0
        } else {
            viewer.findViewById<AppBarLayout>(R.id.appbar).layoutParams.height = defaultToolbarSize
            viewer.findViewById<AppBarLayout>(R.id.appbar).setActivated(true)
            viewer.findViewById<AppBarLayout>(R.id.appbar).setExpanded(true, true)
        }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //generate date strings to ask api about that days
        val currentDayBaseLayout =
            view.findViewById<RecyclerView>(R.id.currentDayRecyclerView)
        currentDayBaseLayout.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        currentDayBaseLayout.adapter = currentDayAdapter(null, "")
        currentDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))
        val upcomingDayBaseLayout = view.findViewById<RecyclerView>(R.id.upcomingDayRecyclerView)
        upcomingDayBaseLayout.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        upcomingDayBaseLayout.adapter = upcomingDayAdapter(null, "")
        upcomingDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))

        if (isTommorow)
            view.findViewById<TextView>(R.id.upcomingDayLessons).text =
                view.findViewById<TextView>(R.id.upcomingDayLessons).text.toString() + getString(
                    R.string.tommorow
                ) + ':'
        else view.findViewById<TextView>(R.id.upcomingDayLessons).text =
            view.findViewById<TextView>(
                R.id.upcomingDayLessons
            ).text.toString() + getString(R.string.monday) + ':'
        //end of amateur commands

        GlobalScope.async(Dispatchers.Main) { //ЕБАТЬ РАБОТАЕТ БЛЯТЬ

            //getting current day data ready
            var currentDayLessonsCount = 0
            for (i in 1..9999999) { //считаем, сколько у нас уроков
                if (currentDaySummary?.getJSONObject("lessons")?.has(i.toString()) == true) {
                    currentDayLessonsCount++
                } else {
                    break
                }
            }

            var currentDayDataSet = arrayOfNulls<Lesson>(currentDayLessonsCount)
            if (currentDayLessonsCount != 0) {
                for (i in 1..currentDayLessonsCount) {
                    val temp =
                        Lesson(
                            currentDaySummary?.getJSONObject("lessons")
                                ?.getJSONObject(i.toString())
                                ?.getString("subject_short"),
                            currentDaySummary?.getJSONObject("lessons")
                                ?.getJSONObject(i.toString())?.getString("mark")
                        )
                    currentDayDataSet[i - 1] = temp
                }
            }

            //getting upcoming day data set ready
            var upcomingDayLessonsCount = 0
            for (i in 1..9999999) {
                if (upcomingDaySummary?.getJSONObject("lessons")?.has(i.toString()) == true) {
                    upcomingDayLessonsCount++
                } else {
                    break
                }
            }
            var upcomingDayDataSet = arrayOfNulls<Lesson>(upcomingDayLessonsCount)
            for (i in 1..upcomingDayLessonsCount) {
                var hometask: String?
                if (upcomingDaySummary?.getJSONObject("lessons")
                        ?.getJSONObject(i.toString())
                        ?.getJSONObject("lesson_data")?.get("hometask")
                        .toString() == null.toString()
                ) {
                    hometask = ""
                } else {
                    hometask = upcomingDaySummary?.getJSONObject("lessons")
                        ?.getJSONObject(i.toString())
                        ?.getJSONObject("lesson_data")?.getJSONObject("hometask")?.getString("text")
                }
                val temp = Lesson(
                    upcomingDaySummary?.getJSONObject("lessons")
                        ?.getJSONObject(i.toString())
                        ?.getString("subject_short"), null,
                    hometask

                )
                upcomingDayDataSet[i - 1] = temp
            }

            //sending our data to render
//            activity?.runOnUiThread {
            currentDayBaseLayout.swapAdapter(
                currentDayAdapter(
                    currentDayDataSet,
                    resources.getString(R.string.no_lessons_today)
                ), true
            )
            upcomingDayBaseLayout.swapAdapter(
                upcomingDayAdapter(
                    upcomingDayDataSet,
                    resources.getString(R.string.no_lessons_tommorow)
                ), true
            )
//            }
        }.start()

    }
}
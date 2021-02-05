package com.neitex.dzennik_bfg.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.CurrentDayAdapter
import com.neitex.dzennik_bfg.shared_functions.DividerItemDecorator
import com.neitex.dzennik_bfg.shared_functions.Lesson
import com.neitex.dzennik_bfg.shared_functions.UpcomingDayAdapter
import io.sentry.Sentry
import org.json.JSONObject


class SummaryPage : Fragment() {
    private var userID: String = null.toString()
    private lateinit var preferences: SharedPreferences
    private lateinit var viewer: View
    private var currentDaySummary: JSONObject? = null
    private var upcomingDaySummary: JSONObject? = null
    private var isTommorow: Boolean = true

    fun initFields(
        usrID: String,
        prefs: SharedPreferences,
        view: View,
        currentSummary: JSONObject,
        upcomingSummary: JSONObject,
        tommorow: Boolean
    ) {
        userID = usrID
        preferences = prefs
        viewer = view
        currentDaySummary = currentSummary
        upcomingDaySummary = upcomingSummary
        isTommorow = tommorow
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (userID == null.toString()) {
            Sentry.captureMessage("Created SummaryPage view without initializing userID")
            throw Exception("Created SummaryPage view without initializing userID")
        }
        val view = inflater.inflate(R.layout.summary_fragment, container, false)
        val currentDayBaseLayout =
            view.findViewById<RecyclerView>(R.id.currentDayRecyclerView)
        currentDayBaseLayout.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        currentDayBaseLayout.adapter = CurrentDayAdapter(null, "")
        currentDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))
        val upcomingDayBaseLayout = view.findViewById<RecyclerView>(R.id.upcomingDayRecyclerView)
        upcomingDayBaseLayout.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        upcomingDayBaseLayout.adapter = UpcomingDayAdapter(null, "")
        upcomingDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //generate date strings to ask api about that days
        val currentDayBaseLayout = view.findViewById<RecyclerView>(R.id.currentDayRecyclerView)
        val upcomingDayBaseLayout = view.findViewById<RecyclerView>(R.id.upcomingDayRecyclerView)
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

        //getting current day data ready
        var currentDayLessonsCount = 0
        for (i in 1..9999999) { //считаем, сколько у нас уроков
            if (currentDaySummary?.getJSONObject("lessons")?.has(i.toString()) == true) {
                currentDayLessonsCount++
            } else {
                break
            }
        }

        val currentDayDataSet = arrayOfNulls<Lesson>(currentDayLessonsCount)
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
        val upcomingDayDataSet = arrayOfNulls<Lesson>(upcomingDayLessonsCount)
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
        currentDayBaseLayout.adapter =
            CurrentDayAdapter(
                currentDayDataSet,
                resources.getString(R.string.no_lessons_today)
            )

        upcomingDayBaseLayout.adapter =
            UpcomingDayAdapter(
                upcomingDayDataSet,
                resources.getString(R.string.no_lessons_tommorow)
            )
    }
}
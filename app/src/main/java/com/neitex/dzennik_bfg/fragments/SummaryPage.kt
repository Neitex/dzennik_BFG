package com.neitex.dzennik_bfg.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.getAvatar
import com.neitex.dzennik_bfg.shared_functions.*
import com.neitex.dzennik_bfg.updateNameText
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class SummaryPage : Fragment() {
    private var currentDaySummary: JSONObject? = null
    private var upcomingDaySummary: JSONObject? = null
    private var isTommorow: Boolean = true
    private var isMarksAllowed = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.summary_fragment, container, false)
        if (arguments != null) {
            currentDaySummary = JSONObject(arguments?.getString("currentDay").toString())
            upcomingDaySummary = JSONObject(arguments?.getString("upcomingDay").toString())
            isTommorow = arguments?.getBoolean("isTomorrow") == true
            val currentDayBaseLayout =
                view.findViewById<RecyclerView>(R.id.currentDayRecyclerView)
            currentDayBaseLayout.layoutManager =
                LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
            currentDayBaseLayout.adapter = CurrentDayAdapter(null, "")
            currentDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))
            val upcomingDayBaseLayout =
                view.findViewById<RecyclerView>(R.id.upcomingDayRecyclerView)
            upcomingDayBaseLayout.layoutManager =
                LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
            upcomingDayBaseLayout.adapter = UpcomingDayAdapter(null, "")
            upcomingDayBaseLayout.addItemDecoration(DividerItemDecorator(resources.getDrawable(R.drawable.gray_divider)))
            isMarksAllowed = arguments?.getBoolean("isMarksAllowed") ?: true
        } else {
            Sentry.captureMessage("Initialized summaryPage without bundle")
            throw Exception("Initialized summaryPage without bundle")
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //generate date strings to ask api about that days
        val currentDayBaseLayout = view.findViewById<RecyclerView>(R.id.currentDayRecyclerView)
        val upcomingDayBaseLayout = view.findViewById<RecyclerView>(R.id.upcomingDayRecyclerView)
        if (isTommorow)
            view.findViewById<TextView>(R.id.upcomingDayLessons).text =
                view.findViewById<TextView>(R.id.upcomingDayLessons).text.toString() + getString(
                    R.string.tomorrow
                ) + ':'
        else view.findViewById<TextView>(R.id.upcomingDayLessons).text =
            view.findViewById<TextView>(
                R.id.upcomingDayLessons
            ).text.toString() + getString(R.string.monday) + ':'
        //end of amateur commands

        //getting current day data ready
        var currentDayLessonsCount = 0
        for (i in 1..Int.MAX_VALUE) { //считаем, сколько у нас уроков
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
                currentDayDataSet[i - 1] = filterLessonData(temp, isMarksAllowed)
            }
        }

        //getting upcoming day data set ready
        var upcomingDayLessonsCount = 0
        for (i in 1..Int.MAX_VALUE) {
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
            upcomingDayDataSet[i - 1] = filterLessonData(temp, isMarksAllowed)
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
        GlobalScope.launch(Dispatchers.IO) {
            val avatarURL = getAvatar()
            if (avatarURL != null) {
                withContext(Dispatchers.Main) {
                    view.findViewById<SimpleDraweeView>(R.id.summaryPupilAvatar)
                        .setImageURI(Uri.parse(avatarURL))
                }
            } else {
                withContext(Dispatchers.Main) {
                    view.findViewById<SimpleDraweeView>(R.id.summaryPupilAvatar).layoutParams = ConstraintLayout.LayoutParams(0,0)



                    view.findViewById<SimpleDraweeView>(R.id.summaryPupilAvatar)
                        .setImageBitmap(null)
                }
            }
            withContext(Dispatchers.Main) {
                updateNameText(view.findViewById(R.id.pupilName))
            }
        }
    }
}
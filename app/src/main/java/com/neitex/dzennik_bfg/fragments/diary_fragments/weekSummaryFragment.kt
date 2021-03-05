package com.neitex.dzennik_bfg.fragments.diary_fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.Day
import com.neitex.dzennik_bfg.shared_functions.WeekDaysAdapter
import com.neitex.dzennik_bfg.shared_functions.filterDayData
import io.sentry.Sentry
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeekSummary : Fragment() {
    private var isMarksAllowed = true
    private lateinit var weekData: WeekSummaryMainPage.WeekViewData
    private var position: Int = 0 //position, relative to current week (0)
    private var hasInformation = false

    fun String.toDate(): Date {
        return SimpleDateFormat("yyyy-MM-dd").parse(this)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = arguments
        if (args == null) {
            Sentry.captureMessage("Created WeekSummary without bundle")
            throw Exception("Created WeekSummary without bundle")
        }
        position = args.getInt("position")
        isMarksAllowed = args.getBoolean("isMarksAllowed", true)
        val view = inflater.inflate(R.layout.week_fragment, container, false)
        val daysRecyclerView = view.findViewById<RecyclerView>(R.id.rero)
        daysRecyclerView.layoutManager = LinearLayoutManager(view.context)
        daysRecyclerView.adapter = WeekDaysAdapter(null, Resources.getSystem().configuration.locale)
        weekData =
            ViewModelProvider(this.requireActivity()).get(WeekSummaryMainPage.WeekViewData::class.java)
        weekData.weeks.observe(viewLifecycleOwner, androidx.lifecycle.Observer { item ->
            if (!hasInformation) {
                if (item.containsKey(position)) {
                    updateData(item[position]!!)
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (weekData.weeks.value?.containsKey(position) == true) {
            updateData(weekData.weeks.value?.get(position)!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasInformation)
            if (weekData.weeks.value?.containsKey(position) == true) {
                Log.d("resume", "duh $position")
                updateData(weekData.weeks.value?.get(position)!!)
            }
    }

    fun updateData(weekJSON: JSONObject) {
        Log.d("ddeee", "updating data@ $position")
        Log.d("pos $position", weekJSON.toString())
        val daysRecyclerView = view?.findViewById<RecyclerView>(R.id.rero)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, position * 7)
        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
        Log.d("pos $position", SimpleDateFormat("yyyy-MM-dd").format(calendar.time))
        var daysArray = emptyArray<Day>()
        for (i in 0..6) { //TODO: Make holiday processing
            val dayDate = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            if (weekJSON.has(dayDate)) {
                if (!weekJSON.getJSONObject(dayDate).getJSONObject("lessons").has("1")) {
                    continue
                }
                val day = Day(
                    dayDate,
                    weekJSON.getJSONObject(dayDate).getJSONObject("lessons")
                )
                daysArray += filterDayData(day, isMarksAllowed)
                Log.d("pos $position", "Added: $day")
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        daysArray.sortedBy { it.date.toDate() }
        Log.d("pos $position", daysArray.joinToString())
        if (daysRecyclerView != null) {
            daysRecyclerView.adapter =
                WeekDaysAdapter(daysArray, Resources.getSystem().configuration.locale)
            (daysRecyclerView.adapter as WeekDaysAdapter).notifyDataSetChanged()
        }
        hasInformation = true
    }
}
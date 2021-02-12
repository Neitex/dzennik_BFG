package com.neitex.dzennik_bfg.fragments.diary_fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.Day
import com.neitex.dzennik_bfg.shared_functions.WeekDaysAdapter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeekSummary : Fragment() {
    private lateinit var liveData: MutableLiveData<MutableMap<String, JSONObject>>
    private var position: Int = 0 //position, relative to current week (0)

    public fun initFields(
        data: MutableLiveData<MutableMap<String, JSONObject>>,
        pos: Int
    ) {
        liveData = data
        position = pos
    }

    fun String.toDate(): Date {
        return SimpleDateFormat("yyyy-MM-dd").parse(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.week_fragment, container, false)
        val daysRecyclerView = view.findViewById<RecyclerView>(R.id.rero)
       daysRecyclerView.layoutManager = LinearLayoutManager(view.context)
        daysRecyclerView.adapter = WeekDaysAdapter(null, Resources.getSystem().configuration.locale)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val daysRecyclerView = view.findViewById<RecyclerView>(R.id.rero)
        val rollCount: Int = 0 //TODO: Make position dynamic
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, rollCount)
        if (calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        calendar.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - calendar[Calendar.DAY_OF_WEEK])
        Log.d("dee", liveData.value.toString())
        Log.d("dee", SimpleDateFormat("yyyy-MM-dd").format(calendar.time))
        val weekJSONObject =
            liveData.value?.get(SimpleDateFormat("yyyy-MM-dd").format(calendar.time))
        var daysArray = emptyArray<Day>()
        for (i in 0..6) { //TODO: Make holiday processing
            val dayDate = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            if (!weekJSONObject?.getJSONObject(dayDate)?.getJSONObject("lessons")?.has("1")!!) {
                continue
            }
            val day = Day(
                dayDate,
                weekJSONObject.getJSONObject(dayDate).getJSONObject("lessons")
            )
            daysArray+=day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        daysArray.sortedBy { it.date.toDate() }
        Log.wtf("dev", daysArray[0].date)
        daysRecyclerView.adapter = WeekDaysAdapter(daysArray, Resources.getSystem().configuration.locale)
    }
}
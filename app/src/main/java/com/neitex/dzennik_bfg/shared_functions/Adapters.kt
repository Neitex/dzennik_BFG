package com.neitex.dzennik_bfg.shared_functions

import android.content.res.Resources
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.changePupil
import com.neitex.dzennik_bfg.fragments.ChangePupilsDialog
import com.neitex.dzennik_bfg.fragments.diary_fragments.WeekSummary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Lesson(
    val lessonName: String? = "aaaaa",
    val lessonMark: String? = null,
    val lessonHometask: String? = null
)
data class Day(
    val date: String,
    val lessons: JSONObject
)

class CurrentDayAdapter(private val dataSet: Array<Lesson?>?, private val noLessons: String) :
    RecyclerView.Adapter<CurrentDayAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val lessonNameTextView: TextView
        val lessonMarkTextView: TextView

        init {
            lessonNameTextView = view.findViewById(R.id.lessonNameText)
            lessonMarkTextView = view.findViewById(R.id.lessonMark)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.lesson_with_mark_row, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (dataSet != null) {
            if (dataSet.isNotEmpty()) {
                viewHolder.lessonNameTextView.text = dataSet[position]?.lessonName
                if (dataSet[position]?.lessonMark.toString() == null.toString()) {
                    viewHolder.lessonMarkTextView.text = ""
                } else {
                    viewHolder.lessonMarkTextView.text =
                        dataSet[position]?.lessonMark.toString()
                }
            } else {
                viewHolder.lessonNameTextView.text = noLessons
                viewHolder.lessonMarkTextView.text = ""
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            dataSet == null -> {
                0
            }
            dataSet.isEmpty() -> {
                1
            }
            else -> {
                dataSet.size
            }
        }
    }

}

class UpcomingDayAdapter(
    private val dataSet: Array<Lesson?>?,
    private val noLessons: String
) :
    RecyclerView.Adapter<UpcomingDayAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val lessonNameTextView: TextView
        val lessonHometaskView: TextView

        init {
            lessonNameTextView = view.findViewById(R.id.lessonName)
            lessonHometaskView = view.findViewById(R.id.lessonHometask)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.upcoming_day_lesson_row, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (dataSet != null) {
            if (dataSet.isNotEmpty()) {
                viewHolder.lessonNameTextView.text = dataSet[position]?.lessonName
                viewHolder.lessonHometaskView.text = dataSet[position]?.lessonHometask
            } else {
                viewHolder.lessonNameTextView.text = noLessons
                viewHolder.lessonHometaskView.text = ""
            }
        }
    }

    override fun getItemCount(): Int {
        return when {
            dataSet == null -> {
                0
            }
            dataSet.isEmpty() -> {
                1
            }
            else -> {
                dataSet.size
            }
        }
    }
}

class ChangePupilsAdapter(
    private val dataSet: JSONArray,
    private val bottomSheet: ChangePupilsDialog
) :
    RecyclerView.Adapter<ChangePupilsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pupilName: TextView
        val pupilAvatar: SimpleDraweeView
        val baseLayout: LinearLayout

        init {
            pupilName = view.findViewById(R.id.changePupilNameText)
            pupilAvatar = view.findViewById(R.id.pupilAvatar)
            baseLayout = view.findViewById(R.id.changePupilRowBaseLayout)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.change_pupil_row, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (dataSet.getJSONObject(position)?.get("photo").toString() != null.toString()) {
            val photo = viewHolder.pupilAvatar
            photo.setImageURI(Uri.parse(dataSet.getJSONObject(position)?.get("photo").toString()))
        } else {
            viewHolder.pupilAvatar.setImageBitmap(null)

        }
        viewHolder.pupilName.text =
            dataSet.getJSONObject(position)?.getString("last_name") + ' ' + dataSet.getJSONObject(
                position
            )?.getString("first_name")
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            viewHolder.pupilName,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        viewHolder.baseLayout.isClickable = true
        viewHolder.baseLayout.setOnClickListener {
            changePupil(position)
            bottomSheet.dismiss()

        }
    }

    override fun getItemCount(): Int {
        return dataSet.length()
    }
}


class WeekPageAdapter(
    fragmentManager: FragmentManager,
    private val liveData: MutableLiveData<MutableMap<String, JSONObject>>
) : FragmentPagerAdapter(
    fragmentManager,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    private val zeroPosition = Int.MAX_VALUE / 2
    override fun getCount(): Int {
        return Int.MAX_VALUE
    }

    override fun getItem(position: Int): Fragment {
        return getFragmentBasedOnPosition(position)
    }

    private fun getFragmentBasedOnPosition(position: Int): Fragment {
        val weekSumm = WeekSummary()
        weekSumm.initFields(liveData, position-zeroPosition)
        return weekSumm
    }
}

class WeekDaysAdapter(val dataSet: Array<Day>?,
val locale: Locale):RecyclerView.Adapter<WeekDaysAdapter.ViewHolder>(){
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayName: TextView
       val dayDiaryRecycler : RecyclerView

        init {
            dayName = view.findViewById(R.id.dayDiaryName)
           dayDiaryRecycler = view.findViewById(R.id.dayDiaryRecycler)
        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.day_diary_row, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if(dataSet!=null) {
            val dayDate = SimpleDateFormat("yyyy-MM-dd").parse(dataSet[position].date)
            val dayName = viewHolder.dayName
            dayName.text = SimpleDateFormat("EEEE, dd MMMM", locale).format(dayDate)

            val lessonsArray = arrayOfNulls<Lesson>(dataSet[position].lessons.length())
            Log.d("dev", "Processing: " + dataSet[position].lessons.toString())
            for (i in 1..dataSet[position].lessons.length()) {
                lessonsArray[i-1] = Lesson(
                    dataSet[position].lessons.getJSONObject(i.toString())
                        .getString("subject_short"),
                    dataSet[position].lessons.getJSONObject(i.toString()).get("mark").toString()
                )
            }
            viewHolder.dayDiaryRecycler.layoutManager =
                LinearLayoutManager(viewHolder.dayDiaryRecycler.context)
            viewHolder.dayDiaryRecycler.adapter = WeekdayLessonsAdapter(lessonsArray.requireNoNulls())
            viewHolder.dayDiaryRecycler.addItemDecoration(DividerItemDecorator(viewHolder.dayDiaryRecycler.context.resources.getDrawable(R.drawable.gray_divider)))
        } else {
            viewHolder.dayDiaryRecycler.layoutManager =
                LinearLayoutManager(viewHolder.dayDiaryRecycler.context)
            viewHolder.dayDiaryRecycler.adapter = WeekdayLessonsAdapter(null)
        }
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            viewHolder.dayName,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
    }

    override fun getItemCount(): Int {
        if (dataSet != null) {
            return dataSet.size
        } else return 0
    }
}

class WeekdayLessonsAdapter(val dataSet: Array<Lesson>?) :
    RecyclerView.Adapter<WeekdayLessonsAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lessonNameTextView: TextView
        val lessonMarkTextView: TextView

        init {
            lessonNameTextView = view.findViewById(R.id.lessonNameText)
            lessonMarkTextView = view.findViewById(R.id.lessonMark)
        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.lesson_with_mark_row, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if(dataSet != null) {
            viewHolder.lessonNameTextView.text = dataSet[position].lessonName
            if (dataSet[position].lessonMark.toString() == null.toString()) {
                viewHolder.lessonMarkTextView.text = ""
            } else {
                viewHolder.lessonMarkTextView.text =
                    dataSet[position].lessonMark.toString()
            }
        }

    }

    override fun getItemCount(): Int {
       if(dataSet == null){
           return 0
       } else return dataSet.size
    }
}
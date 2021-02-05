package com.neitex.dzennik_bfg.shared_functions

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.changePupil
import com.neitex.dzennik_bfg.fragments.ChangePupilsDialog
import org.json.JSONArray

data class Lesson(
    val lessonName: String? = "aaaaa",
    val lessonMark: String? = null,
    val lessonHometask: String? = null
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
            .inflate(R.layout.current_day_lesson_row, viewGroup, false)

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

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
import com.neitex.dzennik_bfg.fragments.changePupilsDialog
import org.json.JSONArray
import java.io.File

data class Lesson(
    val lessonName: String? = "aaaaa",
    val lessonMark: String? = null,
    val lessonHometask: String? = null
)

class currentDayAdapter(private val dataSet: Array<Lesson?>?, private val noLessons: String) :
    RecyclerView.Adapter<currentDayAdapter.ViewHolder>() { //нагло спизжено с d.android.com
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val lessonNameTextView: TextView
        val lessonMarkTextView: TextView

        init {
            lessonNameTextView = view.findViewById(R.id.lessonNameText)
            lessonMarkTextView = view.findViewById(R.id.lessonMark)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.current_day_lesson_row, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
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

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        if (dataSet == null) {
            return 0
        } else if (dataSet.isEmpty()) {
            return 1
        } else {
            return dataSet.size
        }
    }

}

class upcomingDayAdapter(
    private val dataSet: Array<Lesson?>?,
    private val noLessons: String
) :
    RecyclerView.Adapter<upcomingDayAdapter.ViewHolder>() { //нагло спизжено с d.android.com
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val lessonNameTextView: TextView
        val lessonHometaskView: TextView

        init {
            lessonNameTextView = view.findViewById(R.id.lessonName)
            lessonHometaskView = view.findViewById(R.id.lessonHometask)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.upcoming_day_lesson_row, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
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

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        if (dataSet == null) {
            return 0
        } else if (dataSet.isEmpty()) {
            return 1
        } else {
            return dataSet.size
        }
    }
}

class changePupilsAdapter(
    private val dataSet: JSONArray,
    val filesDir: File?,
    val bottomSheet: changePupilsDialog
) :
    RecyclerView.Adapter<changePupilsAdapter.ViewHolder>() { //нагло спизжено с d.android.com

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

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.change_pupil_row, viewGroup, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
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

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataSet.length()
    }
}

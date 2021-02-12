package com.neitex.dzennik_bfg.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.neitex.dzennik_bfg.R
import com.neitex.dzennik_bfg.shared_functions.DividerItemDecorator
import com.neitex.dzennik_bfg.shared_functions.ChangePupilsAdapter
import org.json.JSONArray

class ChangePupilsDialog(private val pupilsArray: JSONArray) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(
            R.layout.change_pupil_bottom,
            container, false
        )
        val basicCardView = v.findViewById<CardView>(R.id.parentCardView)
        basicCardView.setCardBackgroundColor(
            darkenColor(
                basicCardView.cardBackgroundColor.defaultColor
            )
        )
        v.setBackgroundColor(resources.getColor(android.R.color.transparent))
        val pupilsList = v.findViewById<RecyclerView>(R.id.pupilsListRecyclerView)
        pupilsList.layoutManager = LinearLayoutManager(
            view?.context,
            LinearLayoutManager.VERTICAL,
            false
        )
        pupilsList.adapter = ChangePupilsAdapter(pupilsArray, this)
        val beautifulDividerItemDecoration =
            DividerItemDecorator(resources.getDrawable(R.drawable.empty_divider))

        pupilsList.addItemDecoration(beautifulDividerItemDecoration)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }
}

@ColorInt
fun darkenColor(@ColorInt color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    hsv[2] *= 0.7f
    return Color.HSVToColor(hsv)
}
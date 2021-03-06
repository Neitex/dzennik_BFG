package com.neitex.dzennik_bfg.shared_functions

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.neitex.dzennik_bfg.R

fun makeSnackbar(
    view: View,
    errorMessage: String,
    messageColor: Int = view.resources.getColor(R.color.error),
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(view, errorMessage, duration)
        .setBackgroundTint(messageColor).show()
}

fun View.hideKeyboard() {
    val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(windowToken, 0)
}

class DividerItemDecorator(private val mDivider: Drawable) : RecyclerView.ItemDecoration() {
    private val mBounds = Rect()
    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(
                left, parent.paddingTop, right,
                parent.height - parent.paddingBottom
            )
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + Math.round(child.translationY)
            val top = bottom - mDivider.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(canvas)
        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.getChildAdapterPosition(view) == state.itemCount - 1) {
            outRect.setEmpty()
        } else outRect[0, 0, 0] = mDivider.intrinsicHeight
    }
}

fun saveSharedTokenSettings(preferences: SharedPreferences, sharedSettings: Map<String, Boolean>){
    preferences.edit().putBoolean("isShared", true).apply()
    //TODO: ADD SETTINGS SAVING
}
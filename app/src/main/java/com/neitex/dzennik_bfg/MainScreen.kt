package com.neitex.dzennik_bfg

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.neitex.dzennik_bfg.fragments.SummaryPage
import com.neitex.dzennik_bfg.fragments.changePupilsDialog
import com.neitex.dzennik_bfg.shared_functions.findPupils
import kotlinx.coroutines.*
import org.json.JSONArray


private lateinit var preferences: SharedPreferences
private lateinit var view: View
private lateinit var pupilsArray: JSONArray
private var pupilID: String = ""
private lateinit var myFragmentManager: FragmentManager
private lateinit var myLifecycle: Lifecycle


class MainScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        preferences = this.getSharedPreferences("data", MODE_PRIVATE)
        val viewPager = findViewById<ViewPager2>(R.id.page_view)
        view = viewPager.rootView


        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            findViewById(R.id.pupilName),
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
        myFragmentManager = supportFragmentManager
        myLifecycle = lifecycle
        val Tabs = findViewById<TabLayout>(R.id.menuTabs)
        supportFragmentManager
        viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle, true)
        TabLayoutMediator(Tabs, viewPager)
        { tab, position ->
            viewPager.currentItem = position
        }.attach()
        val TabsNames = arrayOf(
            getString(R.string.main_page_name),
            getString(R.string.timetable_name),
            getString(R.string.settings_tab_name)
        )
        for (i in 0..Tabs.tabCount) {
            Tabs.getTabAt(i)?.text = TabsNames[i]
        }
        Tabs.selectTab(Tabs.getTabAt(0))
        loadDataAsync()
    }

    class ViewStateAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        val isLoading: Boolean = false
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun createFragment(position: Int): Fragment {
             when (position) {
                 0 -> {
                     if (!isLoading) {
                         return SummaryPage(pupilID, preferences, view)
                     } else return Fragment()
                 }
                 1 -> {
                     if (!isLoading) {
                         return SummaryPage(pupilID, preferences, view)
                     } else return Fragment()
                 }
                 2 -> {

                     if (!isLoading) {
                         return SummaryPage(pupilID, preferences, view, 2)
                     } else return Fragment()
                 }
                else -> {
                    throw RuntimeException("$position is outside of tabs range")
                }
            }
        }
        override fun getItemCount(): Int {
            return 3
        }
    }


    fun loadDataAsync() = GlobalScope.async(Dispatchers.Main) {
        try {
            findViewById<ProgressBar>(R.id.progressBar2).visibility = ProgressBar.VISIBLE
            withContext(Dispatchers.IO) {
                if (preferences.all["user_type"] == "Parent") {
                    var pupils =
                        findPupils(
                            preferences.all["token"].toString(),
                            preferences.all["id"].toString(),
                            view
                        )
                    if (pupils == null) {
                        this@async.cancel()
                    } else
                        pupilsArray = pupils
                } else if (preferences.all["user_type"] == "Pupil") {
                    pupilID = preferences.all["id"].toString()
                }
            }
            if (preferences.all["user_type"] == "Parent")
                changePupil(0)
            else if (preferences.all["user_type"] == "Pupil")
                updateName()
        } catch (e: Exception) {
        } finally {
            findViewById<ProgressBar>(R.id.progressBar2).visibility = ProgressBar.GONE
        }
    }


}

fun updateName() {
    view.findViewById<TextView>(R.id.pupilName).text =
        preferences.all["last_name"].toString() + ' ' + preferences.all["first_name"].toString()
    view.findViewById<ViewPager2>(R.id.page_view).adapter = null
    view.findViewById<ViewPager2>(R.id.page_view).adapter =
        MainScreen.ViewStateAdapter(myFragmentManager, myLifecycle, false)
    TextViewCompat.setAutoSizeTextTypeWithDefaults(
        view.findViewById(R.id.pupilName),
        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
    )
}


fun changePupil(newPupil: Int) {
    if (newPupil >= pupilsArray.length()) {
        throw IllegalArgumentException("$newPupil (newPupil) is bigger than pupilsArray size (${pupilsArray.length()})")
    }

    if (pupilsArray.getJSONObject(newPupil).getInt("id").toString() != pupilID) {
        if (pupilsArray.length() != 1) {
            view.findViewById<TextView>(R.id.pupilName).setOnClickListener {
                val pupilsDialog = changePupilsDialog(pupilsArray)
                pupilsDialog.show(myFragmentManager, pupilsDialog.tag)
            }
        }
        pupilID = pupilsArray.getJSONObject(newPupil).getInt("id").toString()
        view.findViewById<ViewPager2>(R.id.page_view).adapter = null
        view.findViewById<ViewPager2>(R.id.page_view).adapter =
            MainScreen.ViewStateAdapter(myFragmentManager, myLifecycle, false)
        view.findViewById<TextView>(R.id.pupilName).text =
            pupilsArray.getJSONObject(newPupil)?.getString("last_name") + ' ' +
                    pupilsArray.getJSONObject(newPupil)?.getString("first_name")
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            view.findViewById(R.id.pupilName),
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
        )
    }
}
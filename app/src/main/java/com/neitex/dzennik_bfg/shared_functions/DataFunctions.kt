package com.neitex.dzennik_bfg.shared_functions

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun filterDayData(inputDay: Day, isMarksAllowed: Boolean): Day {
    if (isMarksAllowed) {
        return inputDay
    }
    val filteredJSONObject = inputDay.lessons
    for (i in 0..filteredJSONObject.length()) {
        if (filteredJSONObject.has(i.toString())) {
            val filteredLesson = filteredJSONObject.getJSONObject(i.toString())
            filteredLesson.put("mark", "")
            filteredJSONObject.put(i.toString(), filteredLesson)
        }
    }
    return Day(inputDay.date, filteredJSONObject)
}

fun filterLessonData(lesson: Lesson, isMarksAllowed: Boolean):Lesson{
    if (isMarksAllowed) return lesson
    else return Lesson(lesson.lessonName, null, lesson.lessonHometask)
}

fun filterPupils(pupilsArray: JSONArray, preferences: SharedPreferences): JSONArray {
        val filteredPupilsArray = JSONArray()
        Log.d("filter", "cmon" + filteredPupilsArray.toString(2))
        val allowedUsers = preferences.getStringSet("allowedUsers", null) ?: return pupilsArray
        Log.d("filter", "cmon" + allowedUsers.toString())
        for (i in 0 until pupilsArray.length()) {
            if (allowedUsers.contains(pupilsArray.getJSONObject(i).getInt("id").toString())) {
                filteredPupilsArray.put(pupilsArray.getJSONObject(i))
                Log.d("deev", filteredPupilsArray.toString(2))
            }
        }
        Log.d("filter", "cmon" + filteredPupilsArray.toString(2))
        return filteredPupilsArray
}

fun savePrivacyInfo(preferences: SharedPreferences, privacySettings: JSONObject) {
    val allowedUsersJSONArray = privacySettings.getJSONArray("allowedUsers")
    var allowedUsersSet = mutableSetOf<String>()
    for (i in 0..allowedUsersJSONArray.length()) {
        if (!allowedUsersJSONArray.isNull(i)) {
            allowedUsersSet.add(allowedUsersJSONArray.getString(i))
        }
    }
    preferences.edit()
        .putBoolean("isMarksAllowed", privacySettings.getBoolean("isMarksAllowed")).apply()
    if (allowedUsersSet.size != 0) {
        preferences.edit().putStringSet("allowedUsers", allowedUsersSet).apply()
    }
}
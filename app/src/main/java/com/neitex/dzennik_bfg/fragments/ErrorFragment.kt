package com.neitex.dzennik_bfg.fragments;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment;
import com.neitex.dzennik_bfg.R

class ErrorFragment(val exception: Exception?):Fragment() {
        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View? {
                val view = inflater.inflate(R.layout.error_fragment, container, true)
                if (exception != null){
                        view.findViewById<TextView>(R.id.exceptionString).text ="${getString(R.string.error_info_for_developer)} \n ${exception.localizedMessage ?: "No info :/"} "
                }
                return view
        }

}
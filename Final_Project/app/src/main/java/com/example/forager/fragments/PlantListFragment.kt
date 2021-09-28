package com.example.forager.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R

class PlantListFragment : Fragment() {

    private lateinit var plantRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant, container, false)

        return view
    }

    companion object {
        fun newInstance(): PlantListFragment {
            return PlantListFragment()
        }
    }

}
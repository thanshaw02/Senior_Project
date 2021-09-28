package com.example.forager.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R
import com.example.forager.databinding.FragmentPlantBinding

private const val LOG = "PlantFragment"

class PlantFragment : Fragment() {

    private lateinit var plantName: String
    private lateinit var date: String

    private lateinit var plantRecyclerView: RecyclerView
    private lateinit var binding: FragmentPlantBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val submitBtn = view.findViewById<Button>(resources.getIdentifier("submit_btn", "id", context?.packageName))
        val date = view.findViewById<TextView>(resources.getIdentifier("plant_date", "id", context?.packageName))
        val plantName = view.findViewById<TextView>(resources.getIdentifier("plant_name", "id", context?.packageName))
        submitBtn.setOnClickListener {

        }
    }

    companion object {
        fun newInstance(): PlantFragment {
            return PlantFragment()
        }
    }

}
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

private const val LOG = "PlantFragment"

class PlantFragment : Fragment() {

    private lateinit var plantName: String
    private lateinit var date: String

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

        // This does nothing right now
        // I will want this to save the data entered, and add it to the users "list of plants found"
        // I will autogenerate the date with a new Date() instance
        view.findViewById<Button>(R.id.add_plant_button).setOnClickListener {
            val plantName = view.findViewById<EditText>(R.id.plant_name)
            Log.d(LOG, "Plant name: ${plantName.text}")
            Toast.makeText(activity, "Plant name: ${plantName.text}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): PlantFragment {
            return PlantFragment()
        }
    }

}
package com.example.forager.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R
import com.example.forager.databinding.SinglePlantCardBinding
import com.example.forager.localdata.model.Plant
import com.example.forager.viewmodel.HomeViewModel
import com.example.forager.viewmodel.PlantListsViewModel

private const val LOG = "PlantDatabaseFragment:"

class PlantDatabaseFragment : Fragment() {

    // Lazily initializing my HomeViewModel
    private val homeVM by activityViewModels<HomeViewModel>()

    private lateinit var recyclerView: RecyclerView
    private var plantDBAdapter: PlantDBAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant_database, container, false)

        recyclerView = view.findViewById(R.id.plant_database_RV) as RecyclerView

        // Setting up the RecyclerView's adaptor
        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            plantDBAdapter = PlantDBAdapter(homeVM.getLocalPlantData)
        }
        recyclerView.adapter = plantDBAdapter

        return view
    }

    private inner class PlantDBHolder(
        private val cardItemView: SinglePlantCardBinding
    ) : RecyclerView.ViewHolder(cardItemView.root) {

        private var commonName: TextView
        private var scientificName: TextView
        private var plantImage: ImageView
        private var expandedCommonName: TextView
        private var expandedScientificName: TextView
        private var expandedPlantType: TextView
        private var expandedPlantColor: TextView
        private var expandedPlantSun: TextView
        private var expandedPlantHeight: TextView
        private var currentPlant: Plant? = null

        init {
            commonName = cardItemView.commonNameTV
            scientificName = cardItemView.scientificNameTV
            plantImage = cardItemView.plantImageIV
            expandedCommonName = cardItemView.expandedCommonName
            expandedScientificName = cardItemView.expandedScientificName
            expandedPlantType = cardItemView.plantTypeSpecific
            expandedPlantColor = cardItemView.plantColorSpecific
            expandedPlantSun = cardItemView.plantSunSpecific
            expandedPlantHeight = cardItemView.plantHeightSpecific
        }

        // TODO: Add animation when expanding if possible
        fun bindView(plant: Plant?) {
            currentPlant = plant!!
            commonName.text = plant.commonName
            scientificName.text = homeVM.checkNameLengths(plant)

            if(plant.getIsExpanded()) cardItemView.expandedInfo.visibility = View.VISIBLE
            else cardItemView.expandedInfo.visibility = View.GONE

            expandedCommonName.text = plant.commonName
            expandedScientificName.text = plant.scientificName
            expandedPlantType.text = homeVM.getPlantType(plant.plantType) // Come back to this to find the plant type
            expandedPlantColor.text = plant.plantColor
            expandedPlantSun.text = plant.sun
            expandedPlantHeight.text = plant.height

            cardItemView.dateFound.visibility = View.GONE
            cardItemView.dateFoundET.visibility = View.GONE
            cardItemView.usersNote.visibility = View.GONE
            cardItemView.usersNotesAREA.visibility = View.GONE

            cardItemView.expandDown.setOnClickListener {
                if(plant.getIsExpanded()) {
                    cardItemView.expandedInfo.visibility = View.GONE
                    cardItemView.expandDown.setImageResource(R.drawable.expand_down)
                    plant.setIsExpanded(false)
                }
                else {
                    cardItemView.expandedInfo.visibility = View.VISIBLE
                    cardItemView.expandDown.setImageResource(R.drawable.expand_up)
                    plant.setIsExpanded(true)
                }
            }
        }

    }

    private inner class PlantDBAdapter(
        private val plantList: List<Plant>?
        ) : RecyclerView.Adapter<PlantDBHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantDBHolder {
            val layoutInflater = LayoutInflater.from(activity)
            val cardViewBinding = SinglePlantCardBinding.inflate(layoutInflater)
            return PlantDBHolder(cardViewBinding)
        }

        override fun onBindViewHolder(holder: PlantDBHolder, position: Int) {
            val aPlant = plantList!![position]
            holder.bindView(aPlant)
        }

        override fun getItemCount(): Int = plantList!!.size

    }

    companion object {
        fun newInstance(): PlantDatabaseFragment = PlantDatabaseFragment()
    }

    /**
     *  LifeCycle observing methods
     */

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(LOG, "onAttach() called")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(LOG, "onDetach() called")
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG, "onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG, "onDestroy() called")
    }
}
package com.example.forager.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R
import com.example.forager.databinding.SinglePlantCardBinding
import com.example.forager.databinding.SinglePlantImageViewBinding
import com.example.forager.fragments.PersonalPlantListFragment.Companion.newInstance
import com.example.forager.localdata.model.Plant
import com.example.forager.viewmodel.HomeViewModel

private const val LOG = "PlantDatabaseFragment:"

/**
 * PlantDatabaseFragment that displays the locally stored list of plants that can be found in the area,
 * uses a RecyclerView to display the data.
 *
 * This fragment communicates with [HomeViewModel], retrieving the data from my MySQL Database
 *
 * @see HomeViewModel.getLocalPlantData
 *
 * @author Tylor J. Hanshaw
 */
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
            layoutManager = GridLayoutManager(requireContext(), 2)
            plantDBAdapter = PlantDBAdapter(homeVM.getLocalPlantData)
        }
        recyclerView.adapter = plantDBAdapter

        return view
    }

    /**
     * The holder for the RecyclerView in [PlantDatabaseFragment], all binding of data and widgets
     * is done here.
     *
     * @see bindView
     * @param cardItemView [SinglePlantCardBinding]
     */
    private inner class PlantDBHolder(
//        private val cardItemView: SinglePlantCardBinding
        private var cardItemView: SinglePlantImageViewBinding
    ) : RecyclerView.ViewHolder(cardItemView.root) {

        private var commonName: TextView
        private var scientificName: TextView
        private var plantImage: ImageView
//        private var expandedCommonName: TextView
//        private var expandedScientificName: TextView
//        private var expandedPlantType: TextView
//        private var expandedPlantColor: TextView
//        private var expandedPlantSun: TextView
//        private var expandedPlantHeight: TextView
//        private var currentPlant: Plant? = null

        init {
            commonName = cardItemView.commonName
            scientificName =  cardItemView.scientificName
            plantImage = cardItemView.plantPhoto
//            expandedCommonName = cardItemView.expandedCommonName
//            expandedScientificName = cardItemView.expandedScientificName
//            expandedPlantType = cardItemView.plantTypeSpecific
//            expandedPlantColor = cardItemView.plantColorSpecific
//            expandedPlantSun = cardItemView.plantSunSpecific
//            expandedPlantHeight = cardItemView.plantHeightSpecific
        }

        // TODO: Add animation when expanding if possible
        fun bindView(plant: Plant?) {
            // Using the single plant image view
            commonName.text = plant!!.commonName
            scientificName.text = homeVM.checkNameLengths(plant)
            plantImage.setImageBitmap(plant.getPlantPhotoUri())

            cardItemView.singleCardView.setOnClickListener {
                val layout = layoutInflater.inflate(R.layout.detailed_plant_list_node_view, null)
                layout.findViewById<TextView>(R.id.common_name).text = plant!!.commonName
                layout.findViewById<TextView>(R.id.scientific_name).text = plant.scientificName
                layout.findViewById<TextView>(R.id.plant_type_specific).text = homeVM.getPlantType(plant.plantType!!)
                layout.findViewById<TextView>(R.id.plant_color_specific).text = plant.plantColor
                layout.findViewById<TextView>(R.id.plant_sun_specific).text = plant.sun
                layout.findViewById<TextView>(R.id.plant_height_specific).text = plant.height

                // Hiding the data only needed for found plants
                layout.findViewById<TextView>(R.id.date_found).visibility = View.GONE
                layout.findViewById<TextView>(R.id.date_found_ET).visibility = View.GONE
                layout.findViewById<TextView>(R.id.users_note).visibility = View.GONE
                layout.findViewById<TextView>(R.id.users_notes_AREA).visibility = View.GONE
                val infoBox = AlertDialog.Builder(requireContext())
                infoBox.setCancelable(true).setView(layout)
                    .setCancelable(true)
                    .show()
            }

            // Old way of doing this with no image displayed
//            currentPlant = plant!!
//            commonName.text = plant.commonName
//            scientificName.text = homeVM.checkNameLengths(plant)
//
//            if(plant.getIsExpanded()) cardItemView.expandedInfo.visibility = View.VISIBLE
//            else cardItemView.expandedInfo.visibility = View.GONE
//
//            expandedCommonName.text = plant.commonName
//            expandedScientificName.text = plant.scientificName
//            expandedPlantType.text = homeVM.getPlantType(plant.plantType!!) // Come back to this to find the plant type
//            expandedPlantColor.text = plant.plantColor
//            expandedPlantSun.text = plant.sun
//            expandedPlantHeight.text = plant.height
//
//            cardItemView.dateFound.visibility = View.GONE
//            cardItemView.dateFoundET.visibility = View.GONE
//            cardItemView.usersNote.visibility = View.GONE
//            cardItemView.usersNotesAREA.visibility = View.GONE
//
//            cardItemView.expandDown.setOnClickListener {
//                if(plant.getIsExpanded()) {
//                    cardItemView.expandedInfo.visibility = View.GONE
//                    cardItemView.expandDown.setImageResource(R.drawable.expand_down)
//                    plant.setIsExpanded(false)
//                }
//                else {
//                    cardItemView.expandedInfo.visibility = View.VISIBLE
//                    cardItemView.expandDown.setImageResource(R.drawable.expand_up)
//                    plant.setIsExpanded(true)
//                }
//            }
        }

    }

    /**
     * Prepares each [Plant] to be displayed in a RecyclerView, each plant has a menu when pressed
     * that displays extra data related to the plant. Data includes the plant color, height, sun needed,
     * and the plant type.
     *
     * @see animateView
     * @param plantList [List<Plant>][plantList]
     */
    private inner class PlantDBAdapter(
        private val plantList: List<Plant>?
        ) : RecyclerView.Adapter<PlantDBHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantDBHolder {
            val layoutInflater = LayoutInflater.from(activity)
//            val cardViewBinding = SinglePlantCardBinding.inflate(layoutInflater)
            val cardViewBinding  = SinglePlantImageViewBinding.inflate(layoutInflater)
            return PlantDBHolder(cardViewBinding)
        }

        override fun onBindViewHolder(holder: PlantDBHolder, position: Int) {
            val aPlant = plantList!![position]
            //animateView(holder) // Going to change this to a different animation soon
            holder.bindView(aPlant)
        }

        override fun getItemCount(): Int = plantList!!.size

        // Custom function that deals with animations on my RecyclerView
        fun animateView(viewHolder: RecyclerView.ViewHolder) {
            val recyclerViewAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_inter)
            viewHolder.itemView.animation = recyclerViewAnimation
        }

    }

    companion object {

        /**
         * Use this companion object to create a new instance of a [PlantDatabaseFragment]
         *
         * @return A new instance of fragment PlantDatabaseFragment
         */
        fun newInstance(): PlantDatabaseFragment = PlantDatabaseFragment()
    }
}
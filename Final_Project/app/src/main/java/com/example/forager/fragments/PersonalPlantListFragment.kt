package com.example.forager.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R
import com.example.forager.databinding.SinglePlantCardBinding
import com.example.forager.localdata.model.Plant
import com.example.forager.repository.DataRepository
import com.example.forager.remotedata.PlantListNode
import com.example.forager.remotedata.User
import com.example.forager.viewmodel.HomeViewModel
import com.example.forager.viewmodel.PlantListsViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.w3c.dom.Text
import java.io.Serializable
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

private const val LOG = "PersonalPlantListFragment"

class PersonalPlantListFragment : Fragment() {

    private lateinit var plantListTitle: TextView

    private lateinit var recyclerView: RecyclerView
    private var personalPlantAdaptor: PersonalPlantListAdapter? = null

    // Lazily initializing my HomeViewModel
    private val homeVM by activityViewModels<HomeViewModel>()

    // This is what happens when changes are made to "getNewPlantListNode"
    private val observer = Observer<PlantListNode> {
        personalPlantAdaptor?.updateRecyclerView(it)
    }

    @SuppressLint("LongLogTag") // This is because of the long LOG I am using
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_personal_plant_list, container, false)

        recyclerView = view.findViewById(R.id.personal_plants_RV)
        plantListTitle = view.findViewById(R.id.personal_plant_list_title)
        plantListTitle.text = "${homeVM.getUsersFullName}'s Personal Plant List"

        // Setting up the RecyclerView's adaptor
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 1)
        }

        // This observes any changes made to my "getNewPlantListNode" LiveData
        // See above for the associated Observer
        homeVM.getNewPlantListNode.observeForever(observer)

        // This is mainly here to load the user's past found plants list first
        // Then afterwards all changes to their list is made through the observer above
        homeVM.getPersonalPlantListOfUsers.observe(requireActivity(), { plantFoundList ->
            personalPlantAdaptor = PersonalPlantListAdapter(plantFoundList)
            homeVM.setPersonalListToggled(false)
        })

        recyclerView.adapter = personalPlantAdaptor

        return view
    }

    private inner class PersonalPlantListHolder(
        private var cardItemView: SinglePlantCardBinding
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
        private var expandedPlantNotes: TextView
        private var expandedPlantNotesTitle: TextView
        private var expandedPlantDateFound: TextView
        private var currentPlantNodePlant: Plant? = null

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
            expandedPlantNotes = cardItemView.usersNotesAREA
            expandedPlantNotesTitle = cardItemView.usersNote
            expandedPlantDateFound = cardItemView.dateFoundET
        }

        @SuppressLint("LongLogTag") // For the longer LOG message
        fun bindView(plantNode: PlantListNode?) {
            currentPlantNodePlant = plantNode!!.plantAdded
            commonName.text = plantNode.plantAdded.commonName
            scientificName.text = homeVM.checkNameLengths(plantNode.plantAdded)

            if(plantNode.getIsExpanded()) cardItemView.expandedInfo.visibility = View.VISIBLE
            else cardItemView.expandedInfo.visibility = View.GONE

            // Setting all of the expanded plant data when clicked on
            expandedCommonName.text = plantNode.plantAdded.commonName
            expandedScientificName.text = plantNode.plantAdded.scientificName
            expandedPlantType.text = homeVM.getPlantType(plantNode.plantAdded.plantType)
            expandedPlantColor.text = plantNode.plantAdded.plantColor
            expandedPlantSun.text = plantNode.plantAdded.sun
            expandedPlantHeight.text = plantNode.plantAdded.height
            expandedPlantDateFound.text = plantNode.dateFound

            if(plantNode.plantNotes != "") {
                expandedPlantNotes.text = plantNode.plantNotes
                expandedPlantNotes.isVisible = true
                expandedPlantNotesTitle.isVisible = true
            }

            cardItemView.expandDown.setOnClickListener {
                if(plantNode.getIsExpanded()) {
                    cardItemView.expandedInfo.visibility = View.GONE
                    cardItemView.expandDown.setImageResource(R.drawable.expand_down)
                    plantNode.setIsExpanded(false)
                }
                else {
                    cardItemView.expandedInfo.visibility = View.VISIBLE
                    cardItemView.expandDown.setImageResource(R.drawable.expand_up)
                    plantNode.setIsExpanded(true)
                }
            }
        }
    }

    private inner class PersonalPlantListAdapter(
        private var personalPlantList: MutableList<PlantListNode>
    ) : RecyclerView.Adapter<PersonalPlantListHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalPlantListHolder {
            val layoutInflater = LayoutInflater.from(activity)
            val cardViewBinding = SinglePlantCardBinding.inflate(layoutInflater)
            return PersonalPlantListHolder(cardViewBinding)
        }

        override fun onBindViewHolder(holder: PersonalPlantListHolder, position: Int) {
            val aPlant = personalPlantList[position]
            holder.bindView(aPlant)
        }

        override fun getItemCount(): Int = personalPlantList.size

        // Custom function for updating the user's found plant list
        fun updateRecyclerView(plantListNodeToAdd: PlantListNode) {
            personalPlantList.add(plantListNodeToAdd)
            notifyItemInserted(personalPlantList.size - 1)
        }

    }

    companion object {
        fun newInstance(): PersonalPlantListFragment = PersonalPlantListFragment()
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
        Log.d(LOG, "onStart()")
        super.onStart()
    }

    override fun onPause() {
        Log.d(LOG, "onPause() called")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "onResume() called")
    }

    override fun onStop() {
        // Detach any listeners here
        Log.d(LOG, "onStop() called")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(LOG, "onDestroy() called")
        // Manually removing the observer when "onDestroy" is called
        homeVM.getNewPlantListNode.removeObserver(observer)
        super.onDestroy()
    }
}
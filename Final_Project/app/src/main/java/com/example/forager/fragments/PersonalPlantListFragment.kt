package com.example.forager.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.forager.R
import com.example.forager.databinding.SinglePlantCardBinding
import com.example.forager.databinding.SinglePlantImageViewBinding
import com.example.forager.remotedata.model.PlantListNode
import com.example.forager.repository.MyCallback
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.behavior.SwipeDismissBehavior.SWIPE_DIRECTION_START_TO_END

private const val LOG = "PersonalPlantListFragment"

/**
 * PersonalPlantListFragment that displays the user's list of found plants,
 * uses a RecyclerView to display this list.
 *
 * There are two observers present here, one for the user's list of plants that is obtained from
 * my RealTime Database and the other listens to new plants added from [MapsFragment].
 *
 * Use the [PersonalPlantListFragment.newInstance] companion object to create an instance
 * of this fragment.
 *
 * @see HomeViewModel.getUsersFullName
 * @see HomeViewModel.getNewPlantListNode
 * @see HomeViewModel.observeFoundPlantList
 *
 * @author Tylor J. Hanshaw
 */
// TODO: The list is cut off towards the bottom, you see if more when you expand the extra info menu
// TODO: Same thing is happening with the other plant RecyclerView
class PersonalPlantListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var personalPlantAdaptor: PersonalPlantListAdapter? = null

    private var numPlantsFound = 0

    // Lazily initializing my HomeViewModel
    private val homeVM by activityViewModels<HomeViewModel>()

    // This is what happens when changes are made to "getNewPlantListNode"
    private val observer = Observer<PlantListNode> {
        Log.d(LOG, "New plant incoming: $it")
        personalPlantAdaptor?.updateRecyclerView(it)
    }

    @SuppressLint("LongLogTag") // This is because of the long LOG I am using
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_personal_plant_list, container, false)

        // Using a callback to get the user's plant found count
        // Also using this to display a message to the user if they have no plants added to their list
        /* Made some changes here, I casted the variable passed by the callback and checked for exceptions */
        homeVM.getNumberOfPlantsFound(object: MyCallback {
            override fun getDataFromDB(plantsFound: Any?) {
                val response = plantsFound as String?
                if(response != null) {
                    val noPlants = view.findViewById<TextView>(R.id.no_plants_found)
                    numPlantsFound = plantsFound.toString().toInt()
                    if(numPlantsFound == 0) {
                        recyclerView.visibility = View.GONE
                        noPlants.visibility = View.VISIBLE
                    }
                    else {
                        recyclerView.visibility = View.VISIBLE
                        noPlants.visibility = View.GONE
                    }
                }
                else Log.d( LOG, "Exception when retrieving data from database.")
            }
        })

        recyclerView = view.findViewById(R.id.personal_plants_RV)

        // Setting up the RecyclerView's properties like its layout and animation
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
        }

        // This observes any changes made to my "getNewPlantListNode" LiveData
        // See above for the associated Observer
        homeVM.getNewPlantListNode.observeForever(observer)

        // this allows the user's found plant list to be fully loaded in on log-in
        homeVM.observeFoundPlantList.observe(requireActivity(), { response ->
            personalPlantAdaptor = PersonalPlantListAdapter(response.plants!!)
        })

        ItemTouchHelper(personalPlantAdaptor!!.itemTouchHelper).attachToRecyclerView(recyclerView)

        recyclerView.adapter = personalPlantAdaptor

        return view
    }

    /**
     * The holder for the RecyclerView in [PersonalPlantListFragment], all binding of data and
     * widgets is done here.
     *
     * @see bindView
     * @param cardItemView View binding tied to [SinglePlantCardBinding]
     * @return [RecyclerView.ViewHolder]
     */
    private inner class PersonalPlantListHolder(
//        private var cardItemView: SinglePlantCardBinding
        private var cardItemView: SinglePlantImageViewBinding
    ) : RecyclerView.ViewHolder(cardItemView.root) {

        // These are for the single card view layout
//        private var commonName: TextView
//        private var scientificName: TextView
//        private var plantImage: ImageView
//        private var expandedCommonName: TextView
//        private var expandedScientificName: TextView
//        private var expandedPlantType: TextView
//        private var expandedPlantColor: TextView
//        private var expandedPlantSun: TextView
//        private var expandedPlantHeight: TextView
//        private var expandedPlantNotes: TextView
//        private var expandedPlantNotesTitle: TextView
//        private var expandedPlantDateFound: TextView
//        private var expandedPlantPhoto: ImageView
//        private var currentPlantNodePlant: Plant? = null

        // These are for the single image card view layout
        private var plantPhoto: ImageView
        private var commonName: TextView
        private var scientificName: TextView

        init {
            // For the single image card view
            plantPhoto = cardItemView.plantPhoto
            commonName = cardItemView.commonName
            scientificName = cardItemView.scientificName

            // For the single card view
//            commonName = cardItemView.commonNameTV
//            scientificName = cardItemView.scientificNameTV
//            plantImage = cardItemView.plantImageIV
//            expandedCommonName = cardItemView.expandedCommonName
//            expandedScientificName = cardItemView.expandedScientificName
//            expandedPlantType = cardItemView.plantTypeSpecific
//            expandedPlantColor = cardItemView.plantColorSpecific
//            expandedPlantSun = cardItemView.plantSunSpecific
//            expandedPlantHeight = cardItemView.plantHeightSpecific
//            expandedPlantNotes = cardItemView.usersNotesAREA
//            expandedPlantNotesTitle = cardItemView.usersNote
//            expandedPlantDateFound = cardItemView.dateFoundET
//            expandedPlantPhoto = cardItemView.plantPhoto
        }

        /**
         * Binds the widgets with data passed to it from [PersonalPlantListAdapter],
         * also decides whether the info drop-down window is shown to the user or not.
         *
         * Also handles logic dealing with what information should be shown to the user, and
         * shortens plant Scientific Names depending on their length to help them fit on the screen.
         *
         * Called from the [onBindViewHolder()][PersonalPlantListAdapter.onBindViewHolder] function
         * in the [PersonalPlantListAdapter].
         *
         * @see PersonalPlantListAdapter
         * @see PersonalPlantListAdapter.onBindViewHolder
         * @param plantNode [PlantListNode]
         */
        @SuppressLint("LongLogTag") // For the longer LOG message
        fun bindView(plantNode: PlantListNode?) {
            // For my single image card view
            commonName.text = plantNode!!.plantAdded.commonName
            scientificName.text = homeVM.checkNameLengths(plantNode.plantAdded)
            if(plantNode.plantPhotoUri != null) {
                homeVM.getPlantPhotoFromCloudStorage(plantNode.plantPhotoUri, object: MyCallback {
                    override fun getDataFromDB(data: Any?) {
                        plantPhoto.setImageBitmap(data as Bitmap)
                    }
                })
            }
            cardItemView.singleCardView.setOnClickListener {
                // Opens up a dialog box with all of the information of the given plant displayed
                val layout = layoutInflater.inflate(R.layout.detailed_plant_list_node_view, null)
                layout.findViewById<TextView>(R.id.common_name).text = plantNode.plantAdded.commonName
                layout.findViewById<TextView>(R.id.scientific_name).text = plantNode.plantAdded.scientificName
                layout.findViewById<TextView>(R.id.plant_type_specific).text = homeVM.getPlantType(plantNode.plantAdded.plantType!!)
                layout.findViewById<TextView>(R.id.plant_color_specific).text = plantNode.plantAdded.plantColor
                layout.findViewById<TextView>(R.id.plant_sun_specific).text = plantNode.plantAdded.sun
                layout.findViewById<TextView>(R.id.plant_height_specific).text = plantNode.plantAdded.height
                layout.findViewById<TextView>(R.id.date_found_ET).text = plantNode.dateFound
                layout.findViewById<TextView>(R.id.users_notes_AREA).text = plantNode.plantNotes
                val infoBox = AlertDialog.Builder(requireContext())
                infoBox.setCancelable(true).setView(layout)
                    .setCancelable(true)
                    .show()
            }

            // For my single card view
//            currentPlantNodePlant = plantNode!!.plantAdded
//            commonName.text = plantNode.plantAdded.commonName
//            scientificName.text = homeVM.checkNameLengths(plantNode.plantAdded)
//
//            if(plantNode.getIsExpanded()) cardItemView.expandedInfo.visibility = View.VISIBLE
//            else cardItemView.expandedInfo.visibility = View.GONE
//
//            // Setting all of the expanded plant data when clicked on
//            expandedCommonName.text = plantNode.plantAdded.commonName
//            expandedScientificName.text = plantNode.plantAdded.scientificName
//            expandedPlantType.text = homeVM.getPlantType(plantNode.plantAdded.plantType!!)
//            expandedPlantColor.text = plantNode.plantAdded.plantColor
//            expandedPlantSun.text = plantNode.plantAdded.sun
//            expandedPlantHeight.text = plantNode.plantAdded.height
//            expandedPlantDateFound.text = plantNode.dateFound
//
//            if(plantNode.plantNotes != "") {
//                expandedPlantNotes.text = plantNode.plantNotes
//                expandedPlantNotes.isVisible = true
//                expandedPlantNotesTitle.isVisible = true
//            }
//
//            // It might be faster to cache all photos taken by the user locally
//            // rather than getting it from Firebase storage everytime??
//            // Right now the images do take some time to load, but that's probably okay
//            // since the images are only being seen when the expanded info window is opened.
//            if(plantNode.plantPhotoUri != null) {
//                // Grab the image from the "plantPhotoUri"
//                Log.d(LOG, "Plant photo URI: ${plantNode.plantPhotoUri}")
//                homeVM.getPlantPhotoFromCloudStorage(plantNode.plantPhotoUri, object: MyCallback {
//                    override fun getDataFromDB(data: Any?) {
//                        expandedPlantPhoto.setImageBitmap(data as Bitmap)
//                    }
//                })
//                expandedPlantPhoto.visibility = View.VISIBLE
//            }
//            else expandedPlantPhoto.visibility = View.GONE
//
//            cardItemView.expandDown.setOnClickListener {
//                if(plantNode.getIsExpanded()) {
//                    cardItemView.expandedInfo.visibility = View.GONE
//                    cardItemView.expandDown.setImageResource(R.drawable.expand_down)
//                    plantNode.setIsExpanded(false)
//                }
//                else {
//                    cardItemView.expandedInfo.visibility = View.VISIBLE
//                    cardItemView.expandDown.setImageResource(R.drawable.expand_up)
//                    plantNode.setIsExpanded(true)
//                }
//            }
        }
    }

    /**
     * Prepares each [plant list node][PlantListNode] to get set up and displayed in the RecyclerView.
     * This adapter also has a custom function for updating the list when a user adds a new plant and
     * a function that allows the user to swipe each item to the left, removing the plant from the
     * RecyclerView, their personal plant plist saved locally and remotely, and the associated marker
     * on the Google Map.
     *
     * @see updateRecyclerView
     * @see setSwipeBehavior
     * @see itemTouchHelper
     * @param personalPlantList [PlantListNode]
     */
    private inner class PersonalPlantListAdapter(
        private var personalPlantList: MutableList<PlantListNode>
    ) : RecyclerView.Adapter<PersonalPlantListHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalPlantListHolder {
            val layoutInflater = LayoutInflater.from(activity)
//            val cardViewBinding = SinglePlantCardBinding.inflate(layoutInflater)
            val cardViewBinding = SinglePlantImageViewBinding.inflate(layoutInflater)
            setSwipeBehavior(parent)
            return PersonalPlantListHolder(cardViewBinding)
        }

        override fun onBindViewHolder(holder: PersonalPlantListHolder, position: Int) {
            val aPlant = personalPlantList[position]
            //animateView(holder) // Going to change this to a different animation soon
            holder.bindView(aPlant)
        }

        override fun getItemCount(): Int = personalPlantList.size

        // Custom function for updating the user's found plant list
        fun updateRecyclerView(plantListNodeToAdd: PlantListNode) {
            personalPlantList.add(plantListNodeToAdd)
            notifyItemInserted(personalPlantList.size - 1)
        }

        private fun setSwipeBehavior(view: View) {
            val swipe = SwipeDismissBehavior<CardView>()
            swipe.setSwipeDirection(SWIPE_DIRECTION_START_TO_END)
            swipe.setDragDismissDistance(100F)
            swipe.canSwipeDismissView(view)
        }

        val itemTouchHelper: ItemTouchHelper.SimpleCallback =
            object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            /**
             * Handles swiped events on single views in the RecyclerView. Swiping to the left
             * removes the [PlantListNode] at that position.
             *
             * When a view is removed I must remove it from the RecyclerView, locally cached data,
             * Realtime Database, decrement the user's *number of plants found*, remove the marker,
             * and remove the plant photo from Firebase storage if there is on.
             *
             * @see [HomeViewModel.removePlantFromDB]
             * @see [HomeViewModel.removePlantPhotoFromCloudStorage]
             * @see [HomeViewModel.decrementPlantsFound]
             * @see [HomeViewModel.removeMarker]
             * @param viewHolder Used to get the position of the view being swiped
             * @param direction [Int]
             *
             * @author Tylor J. Hanshaw
             */
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val plantPhotoUrl = personalPlantList[viewHolder.bindingAdapterPosition].plantPhotoUri
                val plantToRemove = personalPlantList[viewHolder.bindingAdapterPosition]
                homeVM.removePlantPhotoFromCloudStorage(plantPhotoUrl)
                homeVM.removePlantFromDB(personalPlantList[viewHolder.bindingAdapterPosition])
                personalPlantList.removeAt(viewHolder.bindingAdapterPosition)
                personalPlantAdaptor!!.notifyItemRangeRemoved(viewHolder.bindingAdapterPosition, 1)
                homeVM.decrementPlantsFound(numPlantsFound)
                homeVM.removeMarker(plantToRemove.lat, plantToRemove.long)
            }

        }

    }

    companion object {

        /**
         * Use this companion object method to create an instance of a [PersonalPlantListFragment]
         *
         * @return A new instance of fragment PersonalPlantList
         */
        fun newInstance(): PersonalPlantListFragment = PersonalPlantListFragment()
    }

    override fun onDestroy() {
        Log.d(LOG, "onDestroy() called")
        // Manually removing the observer when "onDestroy" is called
        homeVM.getNewPlantListNode.removeObserver(observer)
        super.onDestroy()
    }
}
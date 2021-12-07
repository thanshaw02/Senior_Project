package com.example.forager.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.example.forager.activities.FileDirectory
import com.example.forager.R
import com.example.forager.repository.MyCallback
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "MapsFragment"

/**
 * MapsFragment fragment that displays the Google Map for the user.
 * User may 'pin' locations of found plants with a 'long press', when doing so an AlertDialog window
 * will be displayed asking for the needed plant information.
 *
 * Use the [MapsFragment.newInstance] companion object method to create an instance of this fragment.
 *
 * @see [HomeViewModel.getNumberOfPlantsFound]
 *
 * @author Tylor J. Hanshaw
 */
class MapsFragment : Fragment() {

    private lateinit var imageTEST: ImageView

    // Directory where the photos will be saved
    private lateinit var photoDir: File

    private lateinit var photoTakenBM: Bitmap
    private var photoTakenFile: File? = null
    private lateinit var fileDir: FileDirectory

    // For camera operations
    // This handles the returned intent from the cmaer activity
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoTakenFile = photoDir.absoluteFile
            } else {
                Snackbar.make(
                    requireView(),
                    "The photo was not properly received.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    private var numPlantsFound = 0

    private lateinit var toggleBtn: SwitchMaterial

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fileDir = context as FileDirectory
    }

    override fun onDetach() {
        super.onDetach()
    }

    /**
     * Private variable that lazily assigns MapsActivity's ViewModel.
     * @see com.example.forager.MapsActivity
     */
    private val homeVM by activityViewModels<HomeViewModel>()

    /**
     * Private variable of type OnMapReadyCallback
     *
     * @see OnMapReadyCallback
     */
    private val callback = OnMapReadyCallback { googleMap ->

        getResponseUsingCoroutine(googleMap)

        // This is so the zoom in/out buttons are fully visible
        googleMap.setPadding(0, 0, 0, 100)

        // This will toggle all past and new markers on or off
        toggleBtn.setOnCheckedChangeListener { compoundButton, toggled ->
            homeVM.toggleMarkers(toggled)
        }

        getResponseUsingCoroutine(googleMap) // Come back and test this! This may be why the observer was duplicating markers..

        // Setting the styling of GoogleMap
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
        }
        //typeAndStyles.setMapStyle(googleMap, requireContext()) // Sets the style of my map using raw JSON
        // Possibly have a button that hides markers, or hides only some markers, etc.
        googleMap.setOnMapLongClickListener { latlng ->
            setUpDialogBox(latlng, googleMap)
        }

        // Adding marker to Marquette on start just for a point of reference on the map
        // Keeping this for now, but will want to change it so the camera zooms to the user's location
        val marquette = LatLng(46.5436, -87.3954)
        googleMap.addMarker(MarkerOptions().position(marquette).title("Marquette Marker"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marquette, 10f))


    }

    // I'm using this variable to force my Livedata to only update once on startup/log in instead of twice
    var count = 0

    // This is retrieving the user's found plant list

    /**
     * Private function used to set all previous found plant markers onto the map when the user first logs in.
     * Using the [homeVM] variable, I observe the [observeFoundPlantList][HomeViewModel.observeFoundPlantList] LiveData from HomeViewModel to load in all previously saved plant markers.
     * As of now, I am using a crude method to make sure the [numPlantsFound] is never greater than the number of markers the user has.
     * Currently there is a bug where [observeFoundPlantList][HomeViewModel.observeFoundPlantList] is observed twice in a row, duplicating the number of markers for the user.
     *
     * @param googleMap GoogleMap is passed to this function to add the markers to the map.
     */
    private fun getResponseUsingCoroutine(googleMap: GoogleMap) {
        homeVM.observeFoundPlantList.observe(this, { response ->
            response.plants?.forEach { plantNode ->
                if (count < numPlantsFound) {
                    val coords = LatLng(plantNode.lat, plantNode.long)
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(coords)
                            .title(plantNode.plantAdded.commonName)
                            .snippet(plantNode.dateFound)
                    )
                    homeVM.addNewMarker(marker!!)
                    count += 1
                    Log.d(LOG, "Iteration #${count}")
                }
            }
            Log.d(LOG, "Marker count: ${homeVM.markerSize}")
        })
    }

    /**
     * Invoked when a 'long press' is detection on the Google Map.
     * Once invoked an AlertDialog window is opened, and the user is prompted to enter the
     * 'common name' of the plant they have found and some personal notes about the plant.
     *
     * When the user presses the 'submit' button the user's Number of Plants Found is incremented,
     * the plant is added to the user's Personal Plant List, and the Google marker is cached
     * locally in [HomeViewModel].
     *
     * Note: I'm using an AutoFillTextView in this AlertDialog view to auto-guess the plants
     * 'common name' while the user types it in.
     *
     * @param coords Coordinates of plant found
     * @param googleMap Google Map
     * @see HomeViewModel.incrementPlantsFound
     * @see HomeViewModel.addPlantToDB
     * @see HomeViewModel.addNewMarker
     */
    private fun setUpDialogBox(coords: LatLng, googleMap: GoogleMap) {
        val formatter = SimpleDateFormat("MM-dd-yyyy")
        val formattedDate = formatter.format(Calendar.getInstance().time)
        val layout = layoutInflater.inflate(R.layout.fragment_found_plant_form, null)
        val plantName: AutoCompleteTextView = layout.findViewById(R.id.auto_complete_ET)
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_expandable_list_item_1,
            homeVM.getPlantCommonNames()
        )
        plantName.setAdapter(arrayAdapter)

        val takePhotoBtn: ImageButton = layout.findViewById(R.id.take_photo_btn)
        val selectPhotoBtn: ImageButton = layout.findViewById(R.id.select_photo_button)

        takePhotoBtn.setOnClickListener {
            // FileProvider allows the sharing of "content" or photo's in our case to be much more secure
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val fileProvider = FileProvider.getUriForFile(
                requireContext(),
                "com.example.forager.fragments.file_provider",
                photoDir
            )
            takePhotoIntent.apply { putExtra(MediaStore.EXTRA_OUTPUT, fileProvider) }
            try {
                resultLauncher.launch(takePhotoIntent)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    requireView(),
                    "Error opening the Camera app: $e",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        selectPhotoBtn.setOnClickListener {
            // Let the user select a photo from their library??
        }

        val dialogBox = AlertDialog.Builder(requireContext())
        dialogBox.setCancelable(true).setView(layout)
            .setPositiveButton("Submit") { dialog, i ->
                val plantNotes: TextInputEditText = layout.findViewById(R.id.plant_notes_ET)
                val plantToAdd = homeVM.findPlantNode(plantName.text.toString())
                if (plantToAdd != null) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(coords)
                            .title(plantName.text.toString())
                            .snippet(formattedDate)
                    )
                    val plantNodeUid = UUID.randomUUID().toString()


                    // This works so far! But, I think it doesn't load into the RecyclerView because it takes time
                    if (photoTakenFile != null) {
                        homeVM.addPlantPhotoToCloudStorage(
                            photoTakenFile!!.absoluteFile,
                            plantNodeUid,
                            coords,
                            plantToAdd,
                            plantNotes.text.toString()
                        )
                    }
                    homeVM.incrementPlantsFound(numPlantsFound)
                    homeVM.addNewMarker(marker!!)


                } else Snackbar.make(
                    requireView(),
                    "Please enter a valid plant name.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        imageTEST = view.findViewById(R.id.image_TEST)

        toggleBtn = view.findViewById(R.id.toggle_markers)

        photoDir = fileDir.getOutputDirectory(homeVM.getCurrentDate())

        // Getting the user's number of plants found count so I can increment it when adding another plant
        /* Made some changes here, I casted the variable passed by the callback and checked for exceptions */
        homeVM.getNumberOfPlantsFound(object : MyCallback {
            override fun getDataFromDB(data: Any?) {
                val response = data as String?
                if (response != null) {
                    numPlantsFound = data.toString().toInt()
                } else Log.d(LOG, "Exception when loading number of plants found.")
            }
        })

        return view
    }

    /**
     * Overriding onViewCreated() to set up Google Map in the [Google Map Fragment][MapsFragment]
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    companion object {

        /**
         * Use this companion object method to create an instance of a [MapsFragment]
         *
         * @return A new instance of fragment MapsFragment
         */
        @JvmStatic
        fun newInstance(): MapsFragment = MapsFragment()
    }
}
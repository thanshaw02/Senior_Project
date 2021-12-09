package com.example.forager.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.example.forager.misc.TrackingUtility
import com.example.forager.repository.MyCallback
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "MapsFragment"

// Used for location permissions
private const val REQUEST_CODE_LOCATION_PERMISSIONS = 0


class MapsFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private lateinit var photoDir: File
    private var photoTakenFile: File? = null
    private lateinit var fileDir: FileDirectory
    private var numPlantsFound = 0
    private lateinit var myLocation: FusedLocationProviderClient
    private lateinit var toggleBtn: SwitchMaterial
    private val homeVM by activityViewModels<HomeViewModel>()

    // For camera operations
    // This handles the returned intent from the camera activity
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fileDir = context as FileDirectory
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->

        myLocation = FusedLocationProviderClient(requireActivity())
        requestLocationPermissions(googleMap)
        getCurrentPosition(googleMap)

        getResponseUsingCoroutine(googleMap)

        // This is so the zoom in/out buttons are fully visible
        googleMap.setPadding(0, 0, 0, 100)


        // Setting the styling of GoogleMap
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
        }

        // Sets up the "Found plant form" for the user to fill out
        googleMap.setOnMapLongClickListener { latlng ->
            setUpDialogBox(latlng, googleMap)
        }
    }

    // Loads all previous plants found onto the map as markers
    private fun getResponseUsingCoroutine(googleMap: GoogleMap) {
        homeVM.observeFoundPlantList.observe(this, { response ->
            Log.d(LOG, "New plant in MapsFragment: ${response.plants}")
            response.plants?.forEach { plantNode ->
                val coords = LatLng(plantNode.lat, plantNode.long)
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(coords)
                        .title(plantNode.plantAdded.commonName)
                        .snippet(plantNode.dateFound)
                )
                homeVM.addNewMarker(marker!!)
            }
            Log.d(LOG, "Marker count: ${homeVM.markerSize}")
        })
    }

    // Found plant form fragment dialog box
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

        // If the device being used has a camera, then load the camera button
        // Otherwise the camera button won't be there
        if (homeVM.hasCamera!!) {
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
        } else takePhotoBtn.visibility = View.GONE

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
                            .snippet(formattedDate))
                    val plantNodeUid = UUID.randomUUID().toString()

                    // If the device has a camera, then add it with the photo URI in mind
                    // Otherwise add the plant without adding to Firebase storage and downloading the URI
                    if (photoTakenFile != null) {
                        homeVM.addPlantPhotoToCloudStorage(
                            photoTakenFile!!.absoluteFile,
                            plantNodeUid,
                            coords,
                            plantToAdd,
                            plantNotes.text.toString())
                    } else {
                        homeVM.addPlantToDB(
                            coords,
                            plantToAdd,
                            plantNotes.text.toString(),
                            plantNodeUid)
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

        toggleBtn = view.findViewById(R.id.toggle_markers)
        photoDir = fileDir.getOutputDirectory(homeVM.getCurrentDate())

        toggleBtn.setOnCheckedChangeListener { compoundButton, toggled ->
            homeVM.toggleMarkers(toggled)
        }

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
        fun newInstance(): MapsFragment = MapsFragment()
    }

    private fun goToLocation(lat: Double, long: Double, googleMap: GoogleMap) {
        val latLng = LatLng(lat, long)
        val cameraPos = CameraUpdateFactory.newLatLngZoom(latLng, 13F)
        googleMap.moveCamera(cameraPos)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentPosition(googleMap: GoogleMap) {
        myLocation.lastLocation.addOnCompleteListener {
            if(it.isSuccessful) {
                val currentLocation = it.result
                goToLocation(currentLocation.latitude, currentLocation.longitude, googleMap)
            }
        }
    }

    /* REQUESTING LOCATION PERMISSIONS */

    // Suppressing lint here because if the first statement is true then permissions have been granted
    @SuppressLint("MissingPermission")
    private fun requestLocationPermissions(googleMap: GoogleMap? = null) {
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            googleMap!!.isMyLocationEnabled = true
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permission to use this app.",
                REQUEST_CODE_LOCATION_PERMISSIONS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permission to use this app.",
                REQUEST_CODE_LOCATION_PERMISSIONS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else requestLocationPermissions()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) { }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}
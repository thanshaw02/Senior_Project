package com.example.forager.fragments

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.forager.R
import com.example.forager.fragments.FoundPlantFormFragment
import com.example.forager.oldcode.misc.TypeAndStyles
import com.example.forager.remotedata.PlantListNode
import com.example.forager.remotedata.User
import com.example.forager.repository.DataRepository
import com.example.forager.repository.MyCallback
import com.example.forager.repository.login.LoginActivity
import com.example.forager.viewmodel.HomeViewModel

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "MapsFragment"

class MapsFragment : Fragment() {

    private var numPlantsFound = 0

    private lateinit var toggleBtn: SwitchMaterial

    private val homeVM by activityViewModels<HomeViewModel>()

    // This is what I'm using for the style of my map!
    private val typeAndStyles by lazy { TypeAndStyles() }

    private val callback = OnMapReadyCallback { googleMap ->

        // This is so the zoom in/out buttons are fully visible
        googleMap.setPadding(0, 0, 0, 100)

        // This will toggle all past and new markers on or off
        toggleBtn.setOnCheckedChangeListener { compoundButton, toggled ->
            homeVM.toggleMarkers(toggled)
        }

        // This loads all of the found plants for the user on the map when first logging in
        homeVM.getPersonalPlantListOfUsers.observe(this, { plantFoundList ->
            if(homeVM.getHasBeenToggled) {
                plantFoundList.forEach { plantNode ->
                    val coord = LatLng(plantNode.lat, plantNode.long)
                    val marker = googleMap.addMarker(MarkerOptions()
                        .position(coord)
                        .title(plantNode.plantAdded.commonName)
                        .snippet(plantNode.dateFound))
                    homeVM.addUsersCurrentMarkers(marker!!)
                }
                Log.d(LOG, "Number of plants found by user: $numPlantsFound")
                homeVM.setHasBeenToggled(false)
            }
        })

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

    private fun setUpDialogBox(coords: LatLng, googleMap: GoogleMap) {
        val formatter = SimpleDateFormat("MM-dd-yyyy")
        val formattedDate = formatter.format(Calendar.getInstance().time)
        val layout = layoutInflater.inflate(R.layout.fragment_found_plant_form, null)
        val plantName: AutoCompleteTextView = layout.findViewById(R.id.auto_complete_ET)
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_expandable_list_item_1,
            homeVM.getPlantCommonName())
        plantName.setAdapter(arrayAdapter)
        val dialogBox = AlertDialog.Builder(requireContext())
        dialogBox.setCancelable(true).setView(layout)
            .setPositiveButton("Submit") {dialog, i ->
                val plantNotes: TextInputEditText = layout.findViewById(R.id.plant_notes_ET)
                val plantToAdd = homeVM.findPlantNode(plantName.text.toString())
                if(plantToAdd != null) {
                    val marker = googleMap.addMarker(MarkerOptions()
                        .position(coords)
                        .title(plantName.text.toString())
                        .snippet(formattedDate))
                    homeVM.incrementPlantsFound(numPlantsFound)
                    homeVM.addPlantToDB(coords, plantToAdd, plantNotes.text.toString())
                    homeVM.addNewMarker(marker!!)

                    // Don't want to keep this, but this is so the user's plant list will be up to date
                    // If the user doesn't view their list before adding a plant it will go out of sync
                    // but if they view their list before adding a plant, they are fine
                    // I'm adding this in the hopes that it will fix this issue, temporarily
                    homeVM.getPersonalPlantListOfUserInit()
                }
                else Snackbar.make(
                        requireView(),
                        "Please enter a valid plant name.",
                        Snackbar.LENGTH_SHORT).show()
            }.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        toggleBtn = view.findViewById(R.id.toggle_markers)

        // Getting the user's number of plants found count so I can increment it when adding another plant
        homeVM.getNumberOfPlantsFound(object: MyCallback {
            override fun onCallback(plantsFound: String) {
                numPlantsFound = plantsFound.toInt()
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    companion object {
        fun newInstance(): MapsFragment {
            return MapsFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "onResume() called")
    }
    override fun onStart() {
        Log.d(LOG, "onStart() called")
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG, "onDestroy() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG, "onStop() called")
    }
}
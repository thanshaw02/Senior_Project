/*
*   -- So far you can add a plant, the auto-guesser will find the plant you are typing in
*   -- After typing in the plant you founds common name you can submit that and it will be in your personal list of added plants
*       -- But you cannot see your personal plant list quite yet
*   --
* */

package com.example.forager.fragments

import android.content.Context
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.forager.MapsActivity
import com.example.forager.R
import com.example.forager.repository.DataRepository
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.io.Serializable

private const val LOG = "FoundPlantFormFragment"

class FoundPlantFormFragment : Fragment() {

    private lateinit var autoCompleteET: AutoCompleteTextView
    private lateinit var plantNotes: TextInputEditText
//    private lateinit var submitBtn: ExtendedFloatingActionButton
    private var latitude = 0.0
    private var longitude = 0.0
    private var numPlantsFound = 0

    // This will used the shared ViewModel, so all data persists!!!!
    private val homeVM: HomeViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latitude = requireArguments().getDouble(LATITUDE)
        longitude = requireArguments().getDouble(LONGITUDE)
        numPlantsFound = requireArguments().getInt(NUMBER_PLANTS_FOUND)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.slide_left)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_found_plant_form, container, false)

        Log.d(LOG, "Have loaded user's plant markers in: ${homeVM.getHasBeenToggled}")
        autoCompleteET = view.findViewById(R.id.auto_complete_ET)
        plantNotes = view.findViewById(R.id.plant_notes_ET)
//        submitBtn = view.findViewById(R.id.submit_btn)

        // THIS IS BUGGED
        // Everytime someone logs in the list of common names is doubled..
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(requireContext(), android.R.layout.simple_expandable_list_item_1, DataRepository.getPlantCommonName)
        Log.d(LOG, "Array Adapter size: ${arrayAdapter.count}")
        autoCompleteET.setAdapter(arrayAdapter)

        return view
    }

    // This is where I want to maybe call "getNumPlantsFoundAsync()" to update the user's funPlantsFound in the profile view
    override fun onDestroy() {
        super.onDestroy()
        // fragNav?.onPlantSubmit("found_plant_ui")
        Log.d(LOG, "onDestroy() called")
    }

    companion object {
        const val LATITUDE = "com.example.forager.latitude"
        const val LONGITUDE = "com.example.forager.longitude"
        const val NUMBER_PLANTS_FOUND = "com.example.forager.number_plants_found"
        const val GOOGLE_MAP = "com.example.forager.google_map"

        fun newInstance(latitude: Double, longitude: Double, increment: Int): FoundPlantFormFragment {
            val instance = FoundPlantFormFragment().apply {
                arguments = Bundle().apply {
                    putDouble(LATITUDE, latitude)
                    putDouble(LONGITUDE, longitude)
                    putInt(NUMBER_PLANTS_FOUND, increment)
                }
            }
            return instance
        }
    }

    override fun onStop() {
        Log.d(LOG, "onStop() called")
        super.onStop()
    }
}
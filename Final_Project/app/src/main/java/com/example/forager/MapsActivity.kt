/**
 *  TODO: Color theme: https://material.io/resources/color/#!/?view.left=0&view.right=1&primary.color=aade9a&secondary.color=37a187
 *  TODO: Also, I need to start migrating everything over to my "HomeViewModel" like the two PlantLists
 *  TODO: Also need to remove the PlantListsAc
 */

/**
 * TODO: I'm going back to manually setting the menu drawer up with clicks and using the navigation graph
 * TODO: DO NOT use NavigationUI, I don't really like it and it makes it tough to pass data between the fragments..
 */

package com.example.forager

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.GoogleMap
import com.example.forager.databinding.ActivityMapsBinding
import com.example.forager.databinding.ForagerNavigationHeaderBinding
import com.example.forager.fragments.*
import com.example.forager.oldcode.misc.CameraAndViewport
import com.example.forager.oldcode.misc.TypeAndStyles
import com.example.forager.remotedata.User
import com.example.forager.repository.MyCallback
import com.example.forager.repository.login.LoginActivity
import com.example.forager.splashscreens.SplashScreenFragment
import com.google.firebase.auth.FirebaseAuth
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "MapsActivityDEBUG"

class MapsActivity :  AppCompatActivity() {

    // Menu drawer header widgets
    private lateinit var menuHeaderPlantsFound: TextView

    private lateinit var binding: ActivityMapsBinding

    // Menu widgets
    private lateinit var navHeader: ForagerNavigationHeaderBinding
    private lateinit var menuLayout: DrawerLayout
    private lateinit var menuNavView: NavigationView

    // Creating an instance of FriebaseAuth which will hold on to whoever has logged in
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    private lateinit var numPlantsFound: String

    private val numPlantsObserver = Observer<String> {
        Log.d(LOG, "Observation made on number of plants..: $it")
        numPlantsFound = it
    }

    private val userObserver = Observer<User> {
        Log.d(LOG, "User has been updated: $it")
        currentUser = it!!
        numPlantsFound = currentUser.numPlantsFound
    }

    // ViewModel
    private val homeVM by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menu drawer logic
        navHeader = ForagerNavigationHeaderBinding.inflate(layoutInflater)
        menuLayout = binding.drawerLayout
        menuNavView = binding.myNavigationView
        val menuBtn = findViewById<MaterialToolbar>(R.id.nav_bar)

        // This observes any changes made to the user's plant count
        homeVM.getTheNumOfPlants.observeForever(numPlantsObserver)

        // This retrieves the user's data (full name, username, date account was created, etc.)
        homeVM.getUserLiveData.observe(this, userObserver)

        // This is for keep my app in "full screen" mode, sort of
        WindowCompat.setDecorFitsSystemWindows(window, false)

        homeVM.localDataInit(this) // Initializing local data (mainly for the auto-complete feature)

        auth = FirebaseAuth.getInstance()

        // This is to update the mini user area in my menu drawer
        // The first time the menu is opened everything is initialized (full name, username, num plants found)
        // All other times only the amount of plants found is updated
        menuBtn.setNavigationOnClickListener {
            Log.d(LOG, "User's \"numPlantsFound\": ${currentUser.numPlantsFound}")
            menuHeaderPlantsFound = findViewById(R.id.user_plants_found)
            if(homeVM.getFirstTimeClicking) {
                Log.d(LOG, "Menu button pressed.. first time clicking: true")
                val menuHeaderFullName = findViewById<TextView>(R.id.user_full_name)
                val menuHeaderUserName = findViewById<TextView>(R.id.user_username)
                menuHeaderFullName.text = currentUser.fullName
                menuHeaderUserName.text = currentUser.userName
                menuHeaderPlantsFound.text = numPlantsFound
                homeVM.setFirstTimeClick(false)
            }
            else menuHeaderPlantsFound.text = numPlantsFound
            menuLayout.openDrawer(GravityCompat.START) // Opens the menu when pressed
        }

        menuNavView.setNavigationItemSelectedListener { menuItem ->
            menuLayout.closeDrawer(GravityCompat.START)
            goToMenuItem(menuItem.itemId)
            true
        }

        // Opens "fragment_map" when the user logs in
        attachFragment(MapsFragment.newInstance(), "fragment_map")

    }

    // Utility function for fragment navigation
    // Allows the user to move to different fragments without having to recreate them
    // Allows for a more fluid UX
    private fun attachFragment(fragment: Fragment?, tag: String) {
        val manager = supportFragmentManager
        val ft = manager.beginTransaction()
        if(tag == "splash_screen") { // May not keep this part of the code, used for the splash screen
            ft.add(R.id.fragment_container, fragment!!, "splash_screen")
            ft.commit()
        }
        else {
            if(manager.findFragmentByTag(tag) == null) {
                ft.add(R.id.fragment_container, fragment!!, tag)
                ft.addToBackStack(tag)
                ft.commit()
            }
            else {
                for(frag in manager.fragments) {
                    ft.hide(frag)
                }
                ft.show(manager.findFragmentByTag(tag)!!).commit()
            }
        }
    }

    // Handles navigation between fragments via my menu drawer and my navigation graph
    // Trying to show/hide fragments if they are a;ready in the back stack??
    private fun goToMenuItem(menuId: Int) {
        when(menuId) {
            R.id.fragment_map -> attachFragment(MapsFragment.newInstance(), "fragment_map")
            R.id.fragment_groups -> {
                // If I have time I will come back to this
            }
            R.id.fragment_profile -> attachFragment(FragmentProfileMenu.newInstance(), "fragment_profile")
            R.id.fragment_personal_list -> attachFragment(PersonalPlantListFragment.newInstance(), "fragment_personal_list")
            R.id.fragment_plant_database -> attachFragment(PlantDatabaseFragment.newInstance(), "fragment_plant_database")
            R.id.log_out -> logOut()
        }
    }

    // I don't think I need to send an intent to my login page
    // I also don't need to receive and intent, because FirebaseAuth will hold the current user that's already signed in
    private fun logOut() {
        homeVM.logout()
        goToLogin()
    }

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, MapsActivity::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "onResume() called")

        // Checking to see if there is a current user (if someone is logged in)
        if(auth.currentUser == null) {
            goToLogin()
        }
    }

    private fun goToLogin() {
        viewModelStore.clear() // This is supposed to clear all data held in my HomeViewModel
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        Log.d(LOG, "onStart() called")
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        homeVM.getTheNumOfPlants.removeObserver(numPlantsObserver)
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

/*


lifecycleScope.launch {
            delay(4000L)
            marquetteMarker.remove()
        }



        onMapClicked()
        onMapLongClicked()

private fun onMapClicked() {
        map.setOnMapClickListener {
            val latLong = it.latitude.toString() + ", " + it.longitude.toString()
            val toast = Toast.makeText(this@MapsActivity, "Short click on: $latLong", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.TOP, 50, 50)
            toast.show()
        }
    }

    private fun onMapLongClicked() {
        map.setOnMapLongClickListener {
            map.addMarker(MarkerOptions().position(it).title("Your new marker!"))
            val toast = Toast.makeText(this@MapsActivity, "New marker at: ${it.longitude}, ${it.longitude}", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.TOP, 50, 50)
            toast.show()

        }
    }


map.uiSettings.apply {
    isZoomControlsEnabled = true
    // I think I want this enabled, need to come back to this
    // I'll first need to enable my-location layer
    // isMyLocationButtonEnabled = true
}
typeAndStyles.setMapStyle(map, this)

lifecycleScope.launch {
    delay(4000L)

    // animates the zooming in OR out depending on the zoom value given (between 0..20)
    //map.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)

    // This will animate the moving of the camera to a specified location in pixels, with a given duration and a GoogleMap.CancelableCallback
    //map.animateCamera(CameraUpdateFactory.scrollBy(200f, 0f), 2000, null)

    // This animates all of the properties from cameraAndViewport.marquette in CameraAndViewport.kt
    // In this final version I use a GoogleMaps.CancelableCallback as the callback, I override two functions "onFinish()" and "onCancel()" and display a Toast message
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraAndViewport.melbourne), 2000, object: GoogleMap.CancelableCallback {

        // Toast text will display when the camera animation is finished
        override fun onFinish() {
            Toast.makeText(this@MapsActivity, "Finished", Toast.LENGTH_SHORT).show()
        }

        // Toast text will display when/if the camera animation is canceled
        override fun onCancel() {
            Toast.makeText(this@MapsActivity, "Canceled", Toast.LENGTH_SHORT).show()
        }
    })

    //map.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraAndViewport.melbourneBounds, 100), 2000, null)
    // map.setLatLngBoundsForCameraTarget(cameraAndViewport.melbourneBounds)
*/


// This would all go in the "map.uiSettings.apply {  } block

// This is from the last video I watched in the Google API tutorial
// Don't keep this!
// lifecycleScope.launch {
//     delay(4000L)
//     map.moveCamera(CameraUpdateFactory.scrollBy(-1000f, 300f))
// }

// I think I want some padding on the bottom for my buttons and other options
// Keeping this commented so I can come back to it
// map.setPadding(0, 0, 0, 300)
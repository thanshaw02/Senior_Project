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
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.forager.databinding.ActivityMapsBinding
import com.example.forager.databinding.ForagerNavigationHeaderBinding
import com.example.forager.fragments.*
import com.example.forager.remotedata.model.User
import com.example.forager.repository.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.io.File

private const val LOG = "MapsActivityDEBUG"

interface FileDirectory {
    fun getOutputDirectory(fileName: String): File
}

/**
 * Main activity that hosts all four of the main UI fragments, holds the [NavigationView] menu and
 * handles navigation logic.
 *
 * @author Tylor J. Hanshaw
 */
class MapsActivity :  AppCompatActivity(), FileDirectory {

    // Menu drawer header widgets
    private lateinit var menuHeaderPlantsFound: TextView
    private lateinit var menuTitle: TextView

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

    // ViewModel
    private val homeVM by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        menuTitle = findViewById(R.id.menu_title)

        if(checkSystemForCamera()) {
            // Able to take a picture of the plant they have found
            Log.d(LOG, "This device has camera capabilities")
        }
        else {
            // Disable the camera feature for the user
            Log.d(LOG, "This device does not have camera capabilities")
        }

        // Menu drawer logic
        navHeader = ForagerNavigationHeaderBinding.inflate(layoutInflater)
        menuLayout = binding.drawerLayout
        menuNavView = binding.myNavigationView
        val menuBtn = findViewById<MaterialToolbar>(R.id.nav_bar)

        // This uses coroutines to set the logged in user's info
        setUsersInfoWindow()

        // This observes any changes made to the user's plant count
        homeVM.getTheNumOfPlants.observeForever(numPlantsObserver)

        // This is for keep my app in "full screen" mode, sort of
        WindowCompat.setDecorFitsSystemWindows(window, false)

        homeVM.localDataInit(this) // Initializing local data (mainly for the auto-complete feature)

        // This is to update the mini user area in my menu drawer
        // The first time the menu is opened everything is initialized (full name, username, num plants found)
        // All other times only the amount of plants found is updated
        menuBtn.setNavigationOnClickListener {
            Log.d(LOG, "User's \"numPlantsFound\": ${currentUser.numPlantsFound}")
            menuHeaderPlantsFound.text = numPlantsFound
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

    /**
     * Utility function that sets the user's information in the [NavigationView]'s header.
     *
     * When the user logs in data is observed from [HomeViewModel] and is reflected here.
     *
     * @see [HomeViewModel.observeUserInfo]
     *
     * @author Tylor J. Hanshaw
     */
    private fun setUsersInfoWindow() {
        homeVM.observeUserInfo.observe(this, { response ->
            if(response.user != null) {
                currentUser = response.user!!
                numPlantsFound = currentUser.numPlantsFound.toString()
                menuHeaderPlantsFound = findViewById(R.id.user_plants_found)
                val menuHeaderFullName = findViewById<TextView>(R.id.user_full_name)
                val menuHeaderUserName = findViewById<TextView>(R.id.user_username)
                menuHeaderFullName.text = currentUser.fullName
                menuHeaderUserName.text = currentUser.userName
                menuHeaderPlantsFound.text = currentUser.numPlantsFound.toString()
            }
            else Log.e(LOG, "Error retrieving user's data: ${response.exception}")
        })
    }

    // Utility function for creating a file directory for photos taken
    override fun getOutputDirectory(fileName: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpeg", storageDir)
    }

    /**
     * Utility function used to navigate between fragments without removing them from the
     * fragment back-stack. Instead of replacing fragments they are all added once and then
     * swapped around as the user navigates the UI.
     *
     * @param fragment [Fragment]?
     * @param tag Fragment tag
     *
     * @author Tylor J. Hanshaw
     */
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

    /**
     * Utility function that handles click events on each menu item in the [NavigationView] menu.
     *
     * @see [attachFragment]
     * @see [logOut]
     * @param menuId [Int]
     *
     * @author Tylor J. Hanshaw
     */
    private fun goToMenuItem(menuId: Int) {
        when(menuId) {
            R.id.fragment_map -> {
                menuTitle.text = "Home"
                attachFragment(MapsFragment.newInstance(), "fragment_map")
            }
            R.id.fragment_profile -> {
                menuTitle.text = "Profile"
                attachFragment(FragmentProfileMenu.newInstance(), "fragment_profile")
            }
            R.id.fragment_personal_list -> {
                menuTitle.text = "${currentUser.fullName}'s found plants"
                attachFragment(PersonalPlantListFragment.newInstance(), "fragment_personal_list")
            }
            R.id.fragment_plant_database -> {
                menuTitle.text = "Plants"
                attachFragment(PlantDatabaseFragment.newInstance(), "fragment_plant_database")
            }
            R.id.log_out -> logOut()
        }
    }

    /**
     *
     * Checks if the user's device has camera capabilities if the user takes a photo of a plant
     * they find.
     *
     * @see [MapsFragment.resultLauncher]
     * @return [Boolean]
     *
     * @author Tylor J. Hanshaw
     */
    private fun checkSystemForCamera(): Boolean {
        return applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     *  Called when the user manually logs out via the [NavigationView] menu.
     *
     * @see [HomeViewModel.logout]
     * @see [goToLogin]
     *
     * @author Tylor J. Hanshaw
     */
    // I don't think I need to send an intent to my login page
    // I also don't need to receive and intent, because FirebaseAuth will hold the current user that's already signed in
    private fun logOut() {
        homeVM.logout()
        goToLogin()
    }

    companion object {
        /**
         * Use this companion object to create an intent for MainActivity
         *
         * @param context [Context]
         * @return [Intent]
         *
         * @author Tylor J. Hanshaw
         */
        fun newInstance(context: Context): Intent = Intent(context, MapsActivity::class.java)
    }

    /**
     * Checking to see if there is a user currently logged in, in case this activity is
     * reached without a user being logged in.
     *
     * An example of this would be if a user's account is deleted before logging out through
     * the app.
     *
     * @see goToLogin
     * @see [FirebaseAuth.getCurrentUser]
     */
    override fun onResume() {
        super.onResume()
        if(auth.currentUser == null) {
            goToLogin()
        }
    }

    /**
     * Checking to see if there is a user currently logged in, in case this activity is
     * reached without a user being logged in.
     *
     * An example of this would be if a user's account is deleted before logging out through
     * the app.
     *
     * @see goToLogin
     * @see [FirebaseAuth.getCurrentUser]
     */
    override fun onStart() {
        super.onStart()
        if(auth.currentUser == null) {
            goToLogin()
        }
    }

    /**
     * This function is only ever called when [logOut()][logOut] is called.
     *
     * Before destroying this activity and the hosted fragments I clear all cached
     * data in [HomeViewModel].
     *
     * @see [LoginActivity]
     *
     * @author Tylor J. Hanshaw
     */
    private fun goToLogin() {
        viewModelStore.clear() // This is supposed to clear all data held in my HomeViewModel
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Physically removing the observer on the user's *number of plants found*.
     *
     * @see [HomeViewModel.getTheNumOfPlants]
     */
    override fun onDestroy() {
        super.onDestroy()
        homeVM.getTheNumOfPlants.removeObserver(numPlantsObserver)
    }
}
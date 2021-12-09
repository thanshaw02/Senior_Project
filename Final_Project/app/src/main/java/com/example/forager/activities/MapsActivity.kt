/**
 *  TODO: Color theme: https://material.io/resources/color/#!/?view.left=0&view.right=1&primary.color=aade9a&secondary.color=37a187
 *  TODO: Also, I need to start migrating everything over to my "HomeViewModel" like the two PlantLists
 *  TODO: Also need to remove the PlantListsAc
 */

/**
 * TODO: I'm going back to manually setting the menu drawer up with clicks and using the navigation graph
 * TODO: DO NOT use NavigationUI, I don't really like it and it makes it tough to pass data between the fragments..
 */

package com.example.forager.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.forager.R
import com.example.forager.databinding.ActivityMapsBinding
import com.example.forager.databinding.ForagerNavigationHeaderBinding
import com.example.forager.fragments.*
import com.example.forager.remotedata.model.User
import com.example.forager.activities.login.LoginActivity
import com.example.forager.misc.TrackingUtility
import com.google.firebase.auth.FirebaseAuth
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import kotlin.math.sign

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
class MapsActivity : AppCompatActivity(), FileDirectory {

    // Menu drawer header widgets
    private lateinit var menuHeaderPlantsFound: TextView
    private lateinit var menuTitle: TextView
    private lateinit var binding: ActivityMapsBinding

    // Menu widgets
    private var navHeader: ForagerNavigationHeaderBinding? = null
    private lateinit var menuLayout: DrawerLayout
    private var menuNavView: NavigationView? = null
    private lateinit var profilePicture: ImageView
    private lateinit var menuHeaderUserName: TextView

    // Creating an instance of FriebaseAuth which will hold on to whoever has logged in
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    private lateinit var numPlantsFound: String

    private val numPlantsObserver = Observer<String> {
        numPlantsFound = it
    }

    // ViewModel
    private val homeVM by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This is for keep my app in "full screen" mode
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Checks to see if the device has camera capabilities
        homeVM.hasCamera = checkSystemForCamera()

        auth = FirebaseAuth.getInstance()
        menuTitle = binding.menuTitle

        // Menu drawer logic
        navHeader = ForagerNavigationHeaderBinding.inflate(layoutInflater)
        menuLayout = binding.drawerLayout
        menuNavView = binding.myNavigationView
        val toolBar = binding.navBar

        menuHeaderPlantsFound = findViewById(R.id.user_plants_found)

        // This uses coroutines to set the logged in user's info
        setUsersInfoWindow()

        // This observes any changes made to the user's plant count only when a plant is added
        homeVM.getTheNumOfPlants.observe(this, numPlantsObserver)

        homeVM.getProfilePicture.observe(this, {
            Log.d(LOG, "Profile picture changed in MapsActivity")
            Glide.with(this).load(it).into(profilePicture)
        })

        // Initializing local data (mainly for the auto-complete feature)
        homeVM.localDataInit(this)

        // Update meny header with user's information on first log-in
        toolBar.setNavigationOnClickListener {
            menuHeaderPlantsFound.text = numPlantsFound
            menuLayout.openDrawer(GravityCompat.START) // Opens the menu when pressed
        }

        menuNavView!!.setNavigationItemSelectedListener { menuItem ->
            menuLayout.closeDrawer(GravityCompat.START)
            goToMenuItem(menuItem.itemId)
            true
        }

        // Opens "fragment_map" when the user logs in
        attachFragment(MapsFragment.newInstance(), "fragment_map")

        homeVM.getObservedUsernameChanges.observe(this, {
            Log.d(LOG, "Changed username through LiveData: $it")
            menuHeaderUserName.text = it
        })

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
            if (response.user != null) {
                currentUser = response.user!!
                numPlantsFound = currentUser.numPlantsFound.toString()
//                menuHeaderPlantsFound = findViewById(R.id.user_plants_found)
                val menuHeaderFullName = findViewById<TextView>(R.id.user_full_name)
                menuHeaderUserName = findViewById(R.id.user_username)
                menuHeaderFullName.text = auth.currentUser!!.displayName
                menuHeaderUserName.text = currentUser.userName
                menuHeaderPlantsFound.text = currentUser.numPlantsFound.toString()
            } else Log.e(LOG, "Error retrieving user's data: ${response.exception}")
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
        if (manager.findFragmentByTag(tag) == null) {
            ft.add(R.id.fragment_container, fragment!!, tag)
            ft.addToBackStack(tag)
            ft.commit()
        } else {
            for (frag in manager.fragments) {
                ft.hide(frag)
            }
            ft.show(manager.findFragmentByTag(tag)!!).commit()
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
        when (menuId) {
            R.id.fragment_map -> {
                menuTitle.text = "Home"
                attachFragment(MapsFragment.newInstance(), "fragment_map")
            }
            R.id.fragment_profile -> {
                menuTitle.text = "Profile"
                attachFragment(FragmentProfileMenu.newInstance(), "fragment_profile")
            }
            R.id.fragment_personal_list -> {
                menuTitle.text = "Found Plants"
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

    companion object {
        const val SIGN_IN_METHOD = "com.example.forager.sign_in_method"
        /**
         * Use this companion object to create an intent for MainActivity
         *
         * @param context [Context]
         * @return [Intent]
         *
         * @author Tylor J. Hanshaw
         */
        fun newInstance(context: Context, signInMethod: Int): Intent = Intent(context, MapsActivity::class.java).apply {
            putExtra(SIGN_IN_METHOD, signInMethod)
        }
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
        if (auth.currentUser == null) {
            Log.d(LOG, "User is logged in via Google.")
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
}
package com.example.forager

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.lifecycle.Observer
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import androidx.lifecycle.ViewModelProvider
import com.example.forager.databinding.ActivityPlantListsBinding
import com.example.forager.fragments.*
import com.example.forager.remotedata.PlantListNode
import com.example.forager.repository.DataRepository
import com.example.forager.viewmodel.PlantListsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.reflect.GenericArrayType
import java.util.*

private const val LOG = "PlantListsActivity"

class PlantListsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantListsBinding
    private lateinit var bottomNavMenu: BottomNavigationView

    // PlantListsViewModel init
    private val plantListVM by viewModels<PlantListsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        plantListVM.getPersonalPlantListOfUserInit()
        bottomNavMenu = binding.bottomNav

        val fragment = PlantDatabaseFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container2, fragment, "local_plant_database")
            .commit()
        switchMenuItemSelected(1)

        // Bottom navigation menu logic
        bottomNavMenu.menu.getItem(0).setOnMenuItemClickListener {
            // This is the "personal plants" menu item
            val currentFrag = supportFragmentManager.findFragmentById(R.id.fragment_container2)
            if(currentFrag?.tag != "person_plant_list") {
                val fragment = PersonalPlantListFragment.newInstance()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container2, fragment, "person_plant_list")
                    .commit()
                switchMenuItemSelected(0)
                plantListVM.changeMenuOptionToOpen("person_plant_list")
                Log.d(LOG, "Menu to open next: ${plantListVM.getMenuOptionToOpen}")
            }
            true
        }

        // This menu item is the first one that is opened
        bottomNavMenu.menu.getItem(1).setOnMenuItemClickListener {
            // This is the "lookup plants" menu item
            val currentFrag = supportFragmentManager.findFragmentById(R.id.fragment_container2)
            if(currentFrag?.tag != "local_plant_database") {
                val fragment = PlantDatabaseFragment.newInstance()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container2, fragment, "local_plant_database")
                    .commit()
                switchMenuItemSelected(1)
                plantListVM.changeMenuOptionToOpen("local_plant_database")
                Log.d(LOG, "Menu to open next: ${plantListVM.getMenuOptionToOpen}")
            }
            true
        }

        bottomNavMenu.menu.getItem(2).setOnMenuItemClickListener {
            Log.d(LOG, "Going home..")
            plantListVM.menuItemSelected = 1
            val intent = MapsActivity.newInstance(this)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // Instead of destroying this activity this just moves in behind the new activity
            startActivity(intent)
            //finish()

            true
        }

    }

    // Switching between menu options using the PlantListsViewModel
    private fun switchMenuItemSelected(menuItemToSelect: Int) {
        bottomNavMenu.menu.getItem(plantListVM.getSelectedMeuItem(bottomNavMenu, menuItemToSelect)).isChecked = false
        bottomNavMenu.menu.getItem(plantListVM.menuItemSelected).isChecked = true
    }

    companion object {
        fun newInstance(context: Context): Intent {  return Intent(context, PlantListsActivity::class.java) }
    }

    override fun onStart() {
        Log.d(LOG, "onStart()")
        plantListVM.getPersonalPlantListOfUserInit()
        super.onStart()
    }

    override fun onPause() {
        Log.d(LOG, "onPause() called")
        super.onPause()
    }

    override fun onStop() {
        // Detach any listeners here
        Log.d(LOG, "onStop() called")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(LOG, "onDestroy() called")
        super.onDestroy()
    }
}
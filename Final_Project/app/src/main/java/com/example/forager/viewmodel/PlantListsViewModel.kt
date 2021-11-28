package com.example.forager.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.example.forager.localdata.model.Plant
import com.example.forager.remotedata.PlantListNode
import com.example.forager.remotedata.User
import com.example.forager.repository.DataRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot

private const val LOG = "PlantListsViewModel"

// Possibly turn this back into a class? I just love dealing with objects over classes right now lol
// But use a class so I can have some context with a constructor and then grab the local data in the init {...} block
class PlantListsViewModel : ViewModel() {

    private var dataRepo: DataRepository? = null

    init {
        if(dataRepo == null) {
            dataRepo = DataRepository
        }
    }

                        /* MENU OPTIONS CODE && FRAGMENT TO OPEN LOGIC */
    private var menuOptionToOpen = "local_plant_database"
    val getMenuOptionToOpen get() = menuOptionToOpen
    fun changeMenuOptionToOpen(menuOption: String) {
        menuOptionToOpen = menuOption
    }

    var menuItemSelected: Int = 1

    fun getSelectedMeuItem(menu: BottomNavigationView, menuItemToSelect: Int): Int {
        menuItemSelected = menuItemToSelect
        val navView = menu.menu
        for(i in 0..navView.size()) {
            val menuItem = navView.getItem(i)
            if(menuItem.isChecked) return i
        }
        return -1
    }

    // Utility function for changing the scientific name for a given plane based on its length
    fun checkNameLengths(plant: Plant): String {
        if(plant.commonName.length > 19 && plant.scientificName.length > 20) {
            var shortenedScientificName = ""
            for(letter in 0..10) shortenedScientificName += plant.scientificName[letter]
            return "$shortenedScientificName..."
        }
        return plant.scientificName
    }


    /**
     *                                          PERSONAL PLANT LIST
     *
     *  - So this does not work as intended, reference the notes I left above the corresponding methods in DataRepository
     *  - "plantList" is null when I try to log whatever it is holding
     */
    private val userPersonalList: MutableLiveData<List<PlantListNode>> = MutableLiveData<List<PlantListNode>>()



    fun getPersonalPlantListOfUserInit() { DataRepository.getPersonalPlantListOfUser() }
    private fun transformPersonalListOfUser(list: MutableList<DataSnapshot>): LiveData<MutableList<PlantListNode>> {
        val tempListOfUsersPlants: MutableList<PlantListNode> = mutableListOf()
        list.forEach { value ->
            tempListOfUsersPlants.add(
                PlantListNode(
                    value.child("lat").value.toString().toDouble(),
                    value.child("long").value.toString().toDouble(),
                    Plant(
                        value.child("plantAdded").child("commonName").value.toString(),
                        value.child("plantAdded").child("scientificName").value.toString(),
                        value.child("plantAdded").child("plantType").value.toString().toInt(),
                        value.child("plantAdded").child("plantColor").value.toString(),
                        value.child("plantAdded").child("sun").value.toString(),
                        value.child("plantAdded").child("height").value.toString()
                    ),
                    value.child("plantNotes").value.toString()
                )
            )
        }
        return MutableLiveData(tempListOfUsersPlants)
    }
    private val personalPlantListOfUser: LiveData<MutableList<PlantListNode>> = Transformations.switchMap(DataRepository.getPersonalPlantListOfUser) {
        Log.d(LOG, "User's list is being updated..")
        transformPersonalListOfUser(it)
    }
    val getPersonalPlantListOfUsers: LiveData<MutableList<PlantListNode>> get() = personalPlantListOfUser

    // Getting the user's first name
    val getUsersFullName get() = DataRepository.getUsersFullName()


    /* LOCAL PLANT DATA CODE */
//    fun getLocalPlantData(context: Context): MutableList<Plant> {
//        return DataRepository.getLocalPlantData(context)
//    }

    val getLocalPlantData: MutableList<Plant> get() = DataRepository.getLocalPlantData

}
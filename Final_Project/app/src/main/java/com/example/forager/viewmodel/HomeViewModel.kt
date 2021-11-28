/**
 * - LiveData here that gets the data from the DataRepository
 * - That then gets the data from the DB's
 */

package com.example.forager.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.forager.localdata.model.Plant
import com.example.forager.remotedata.PlantListNode
import com.example.forager.remotedata.User
import com.example.forager.repository.DataRepository
import com.example.forager.repository.MyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "HomeViewModel"

class HomeViewModel : ViewModel() {

    init {
        DataRepository.getPersonalPlantListOfUser()
        DataRepository.getTheUserFromFirebase()
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("MM-dd-yyyy")
        return formatter.format(Calendar.getInstance().time)
    }

    /**
     *                      CODE BELOW IS WORKING AS INTENDED
     */

    /**
     *                          Local Data Operations
     */

    val getLocalPlantData: MutableList<Plant> get() = DataRepository.getLocalPlantData

    // Initializing local data
    fun localDataInit(context: Context) {
        DataRepository.getLocalPlantData(context)
    }

    /**
     *                           Remote Data Operations
     */

    /**********************************************************************************************/

    // This is a LiveData of a new PlantListNode being added
    private val newPlantListNode: MutableLiveData<PlantListNode> = MutableLiveData()
    val getNewPlantListNode: LiveData<PlantListNode> get() = newPlantListNode

    fun addPlantToDB(latLng: LatLng, plantToAdd: Plant, plantNotes: String) {
        DataRepository.addPlantLocation(latLng, plantToAdd, plantNotes)
        val newNode = PlantListNode(
            latLng.latitude,
            latLng.longitude,
            plantToAdd,
            plantNotes,
            getCurrentDate()
        )
        newPlantListNode.value = newNode
        Log.d(LOG, "New node has been added to \"newPlantListNode\" LiveData: $newNode")
    }

    fun findPlantNode(plantName: String): Plant? {
        return DataRepository.findPlantNode(plantName)
    }

    // Working with callbacks
    // This gets the uer's number of plants found count using a custom callback interface
    fun getNumberOfPlantsFound(callback: MyCallback) {
        DataRepository.readData(callback)
    }

    /**********************************************************************************************/

    /**********************************************************************************************/
    // This works, but doesn't look great
    // Keep for now, but if I have time come back and optimize this
    private var databaseHolder = FirebaseDatabase.getInstance()
    fun deleteUserAccount(email: String, password: String, user: FirebaseUser) {
        user.reauthenticate(EmailAuthProvider.getCredential(email, password))
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    Log.d(LOG, "User was re-authenticated")
                    DataRepository.deleteUserFromDB(user, databaseHolder.reference)
                    //DataRepository.signOut()
                    DataRepository.deleteUserAuth(user)
                }
                else Log.d(LOG, "User was not re-authenticated")
        }
    }

    /**********************************************************************************************/

    /**********************************************************************************************/

    // TODO: WORKING FOR NOW
    private val transformNumPlantsFound: MutableLiveData<String> = MutableLiveData()
    val getTheNumOfPlants: LiveData<String> get() = transformNumPlantsFound
    fun incrementPlantsFound(inc: Int) {
        viewModelScope.launch {
            transformNumPlantsFound.postValue((inc + 1).toString())
            DataRepository.incrementPlantsFound(inc + 1)
        }
    }
    /**********************************************************************************************/

    /**********************************************************************************************/

    // These 15 lines of code load the logged in user's data from DataRepository when they first log in
    private fun transformCurrentUserSnapshot(userDS: DataSnapshot): LiveData<User> {
        return MutableLiveData(
            User(
                userDS.child("userName").value.toString(),
                userDS.child("fullName").value.toString(),
                userDS.child("email").value.toString(),
                userDS.child("dateCreated").value.toString(),
                userDS.child("numPlantsFound").value.toString()
            ))
    }
    private val userLiveData: LiveData<User> =
        Transformations.switchMap(DataRepository.getUser) { userDS ->
            transformCurrentUserSnapshot(userDS)
    }
    val getUserLiveData: LiveData<User> get() = userLiveData

    private var firstTimeClicking = true
    val getFirstTimeClicking get() = firstTimeClicking
    fun setFirstTimeClick(set: Boolean) {
        firstTimeClicking = set
    }
    /**********************************************************************************************/

    /**********************************************************************************************/

                                /* Handling the toggling of markers */
/**************************************************************************************************/

    // This is for loading the user's list of plants found for the first time
    private var personalListForFirstTime = true
    val getPersonalListForFirstTime get() = personalListForFirstTime
    fun setPersonalListToggled(toggle: Boolean) {
        personalListForFirstTime = toggle
    }

    // Holds the state of the marker toggle
    private var hasBeenToggled = true // True is toggled on, false is toggle of
    val getHasBeenToggled get() = hasBeenToggled
    fun setHasBeenToggled(toggle: Boolean) {
        hasBeenToggled = toggle
    }

    // This gets the user's list of plants found and re-creates the markers on the map
    private val plantsFoundMarkers: MutableList<Marker> = mutableListOf()
    val getPlantsFoundMarkers get() = plantsFoundMarkers

    fun addNewMarker(marker: Marker) {
        plantsFoundMarkers.add(marker)
    }

    fun addUsersCurrentMarkers(marker: Marker) {
        plantsFoundMarkers.add(marker)
    }

    private fun clearMarkers() { plantsFoundMarkers.clear() }

    fun toggleMarkers(toggled: Boolean) {
        Log.d(LOG, "List of markers size: ${plantsFoundMarkers.size}")
        var count = 0
        plantsFoundMarkers.forEach { marker ->
            Log.d(LOG, "Marker #${count++}: ${marker.title}")
            marker.isVisible = toggled
        }
    }

/**************************************************************************************************/

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
                    value.child("plantNotes").value.toString(),
                    value.child("dateFound").value.toString()
                )
            )
        }
        return MutableLiveData(tempListOfUsersPlants)
    }
    private val personalPlantListOfUser: LiveData<MutableList<PlantListNode>> =
        Transformations.switchMap(DataRepository.getPersonalPlantListOfUser) {
            Log.d(LOG, "User's list is being updated..")
            transformPersonalListOfUser(it)
    }
    val getPersonalPlantListOfUsers: LiveData<MutableList<PlantListNode>> get() = personalPlantListOfUser

    // Getting the user's first name
    val getUsersFullName get() = DataRepository.getUsersFullName()

    /**********************************************************************************************/

    /**********************************************************************************************/

    fun logout() {
        DataRepository.clearOldListData()

        clearMarkers()
        setHasBeenToggled(true)
        setFirstTimeClick(true)
        DataRepository.signOut()
    }

    /**********************************************************************************************/

    /**********************************************************************************************/

    /**
     *  Utility functions, moved from my View's to my ViewModel to further strip down the logic in my View's
     */

    // Checks the lengths of both the common and scientific names of each plant
    // Shortens the scientific names down depending on how long they are
    fun checkNameLengths(plant: Plant): String {
        if(plant.commonName.length > 19 && plant.scientificName.length > 20) {
            var shortenedScientificName = ""
            for(letter in 0..10) shortenedScientificName += plant.scientificName[letter]
            return "$shortenedScientificName..."
        }
        return plant.scientificName
    }

    // This translate the type of each from, plant types are represented as integers in my database
    fun getPlantType(plantType: Int): String {
        return when(plantType) {
            0 -> "Wildflower"
            1 -> "Fern"
            2 -> "Tree/Shrub/Vine"
            3 -> "Grasses/Sedges/Rushes"
            else -> "Unknown"
        }
    }

    /**********************************************************************************************/

    /**********************************************************************************************/

    fun getPlantCommonName(): MutableList<String> = DataRepository.getPlantCommonName

    /**********************************************************************************************/

    /**********************************************************************************************/
}

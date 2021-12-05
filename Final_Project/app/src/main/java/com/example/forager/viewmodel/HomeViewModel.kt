/**
 * - LiveData here that gets the data from the DataRepository
 * - That then gets the data from the DB's
 */

package com.example.forager.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.example.forager.localdata.model.Plant
import com.example.forager.remotedata.model.PlantListNode
import com.example.forager.repository.DataRepository
import com.example.forager.repository.MyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val LOG = "HomeViewModel"

/**
 * Main [ViewModel] that handles all data transactions between the Views and [DataRepository].
 * This includes remote data ([FirebaseDatabase]) and local data (SQLite).
 *
 * Some data is cached locally here as well.
 *
 * @author Tylor J. Hanshaw
 */
class HomeViewModel : ViewModel() {

    /**
     *                          Local Data Operations
     */

    val getLocalPlantData: MutableList<Plant> get() = DataRepository.getLocalPlantData

    /**
     * Initializing the SQLite database to be used in the fragment PlantDatabaseFragment.
     *
     * @see [com.example.forager.MapsActivity]
     * @see [DataRepository.getLocalPlantData]
     * @param context [Context]
     */
    fun localDataInit(context: Context) {
        DataRepository.getLocalPlantData(context)
    }

    /**
     * Initializes all plant common names from the local SQLite database. Used for the
     * AutoCompleteTextView in [MapsFragment][com.example.forager.fragments.MapsFragment],
     * specifically when the user is filling out the *found plant form AlertDialog*.
     *
     * @see [com.example.forager.fragments.MapsFragment.setUpDialogBox]
     * @return [MutableList][String]
     */
    fun getPlantCommonNames(): MutableList<String> = DataRepository.getPlantCommonName

    /**
     *                           Remote Data Operations
     */

    /**********************************************************************************************/

    private val auth = FirebaseAuth.getInstance()

    private val photoTakenOfPlant: MutableLiveData<Bitmap> = MutableLiveData()
    val getPhotoTakenOfPlant: LiveData<Bitmap> get() = photoTakenOfPlant

    fun addPhotoTakenOfPlant(photo: Bitmap) {
        photoTakenOfPlant.value = photo
    }

    // These two functions and LiveData handle adding or removing a plant from/to the database
    private val newPlantListNode: MutableLiveData<PlantListNode> = MutableLiveData()
    val getNewPlantListNode: LiveData<PlantListNode> get() = newPlantListNode

    /**
     * Physically adds a new [PlantListNode] to the user's *personal plant list* in my
     * Realtime Database.
     *
     * @see [newPlantListNode]
     * @see DataRepository.addPlantLocation
     * @param latLng [LatLng]
     * @param plantToAdd [Plant]
     * @param plantNotes [String]
     *
     * @author Tylor J. Hanshaw
     */
    // This is how I want to add a plant to the database, by creating the PlantListNode in the ViewModel, NOT the View
    fun addPlantToDB(
        latLng: LatLng,
        plantToAdd: Plant,
        plantNotes: String,
        plantNodeUid: String
    ) {
        val newNode = PlantListNode(
            latLng.latitude,
            latLng.longitude,
            plantToAdd,
            plantNotes,
            getCurrentDate(),
            "/plant_photos/${FirebaseAuth.getInstance().currentUser!!.uid}/$plantNodeUid",
            plantNodeUid
        )
        DataRepository.addPlantLocation(newNode)
        newPlantListNode.postValue(newNode)
    }

    /**
     * Physically removes a [PlantListNode] from the user's *personal plant list* in my
     * Realtime Database.
     *
     * @see [DataRepository.removePlantFromDB]
     * @param plantToRemove [PlantListNode]
     *
     * @author Tylor J. Hanshaw
     */
    fun removePlantFromDB(plantToRemove: PlantListNode) {
        DataRepository.removePlantFromDB(plantToRemove.getUID())
    }

    // These two functions and LiveData incrementing and decrementing the user's number of plants found
    private val transformNumPlantsFound: MutableLiveData<String> = MutableLiveData()
    val getTheNumOfPlants: LiveData<String> get() = transformNumPlantsFound

    /**
     * Increments the user's number of plants found asynchronously using the
     * *ViewModel coroutine scope* in my Realtime Database and locally.
     *
     * @see transformNumPlantsFound
     * @see [DataRepository.incrementPlantsFound]
     * @param inc [Int]
     *
     * @author Tylor J. Hanshaw
     */
    fun incrementPlantsFound(inc: Int) {
        viewModelScope.launch {
            transformNumPlantsFound.postValue((inc + 1).toString())
            DataRepository.incrementPlantsFound(inc + 1)
        }
    }

    /**
     * Decrements the user's number of plants found asynchronously using the
     * *ViewModel coroutine scope* in my Realtime Database and locally.
     *
     * @see transformNumPlantsFound
     * @see [DataRepository.decrementPlantsFound]
     * @param dec [Int]
     *
     * @author Tylor J. Hanshaw
     */
    fun decrementPlantsFound(dec: Int) {
        viewModelScope.launch {
            transformNumPlantsFound.postValue((dec - 1).toString())
            DataRepository.decrementPlantsFound(dec - 1)
        }
    }

    /**
     * Adds the photo taken of the user's new plant to Firebase's cloud storage. Use the
     * *URL* reference in storage and store this into the user's *plant list node* node in
     * my Realtime Database.
     *
     * @param photo [Bitmap]
     * @param userUid [String]
     *
     * @author Tylor J. Hanshaw
     */
    fun addPlantPhotoToCloudStorage(photo: File?, plantUid: String) {
        viewModelScope.launch {
            DataRepository.addPlantPhotoToCloudStorage(photo, plantUid)
        }
    }

    /**
     * Used in *PersonalPlantFragment* when a plant is swiped left and removed.
     *
     * @see [DataRepository.deletePlantPhotoFromCloudStorage]
     * @see [com.example.forager.fragments.PlantDatabaseFragment.PlantDBAdapter]
     * @param plantPhotoUrl File path of the photo that needs to be deleted
     *
     * @author Tylor J. Hanshaw
     */
    fun removePlantPhotoFromCloudStorage(plantPhotoUrl: String?) {
        if(plantPhotoUrl != null) {
            viewModelScope.launch(Dispatchers.IO) {
                DataRepository.deletePlantPhotoFromCloudStorage(plantPhotoUrl)
            }
        }
        else Log.d(LOG, "This plant entry has no photo associated with it.")
    }

    /**
     * Used to get a plant photo from the Firebase storage for displaying in the
     * [PersonalPlantListFragment][com.example.forager.fragments.PlantDatabaseFragment]
     *
     * @see [DataRepository.getPlantPhotoFromCloudStorage]
     * @param plantPhotoUri File path of the photo needed for the
     * [PersonalPlantList][com.example.forager.fragments.PlantDatabaseFragment].
     * @param callback [MyCallback] interface used as a callback function
     *
     * @author Tylor J. Hanshaw
     */
    fun getPlantPhotoFromCloudStorage(plantPhotoUri: String?, callback: MyCallback) {
        if(plantPhotoUri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                DataRepository.getPlantPhotoFromCloudStorage(plantPhotoUri, callback)
            }
        }
        else Log.d(LOG, "This plant entry has no photo associated with it.")

    }

    /**
     * Retrieves the user's number of plants found from my Realtime Database using
     * a *callback function instead of a coroutine*.
     *
     * @see [DataRepository.getNumberOfPlantsFound]
     * @param callback [MyCallback]
     *
     * @author Tylor J. Hanshaw
     */
    fun getNumberOfPlantsFound(callback: MyCallback) {
        DataRepository.getNumberOfPlantsFound(callback)
    }

    /**
     * Retrieves the user's full name from my Realtime Database using
     * a *callback function instead of a coroutine*.
     *
     * @see [DataRepository.getUsersFullName]
     * @param callback [MyCallback]
     *
     * @author Tylor J. Hanshaw
     */
    fun getUsersFullName(callback: MyCallback) {
        DataRepository.getUsersFullName(callback)
    }

    // Not currently using this, and probably won't in the future
    // But this was be trying to fix a bug
    // The bug is that if you add a plant before opening your personal list, the plant added won't be seen until you re-log
    // The issue is happening because the observer that observes new nodes added isn't set up until you open your list
    // So if you open your list and then add plants you will see those added, but if you don't open your list they won't show up
    fun getUsersPreviousPlantsFoundList(callback: MyCallback) {
        DataRepository.getUsersPreviousPlantsFoundList(callback)
    }


                        /* Logging out/removing a user's account operations */
    /**********************************************************************************************/
    // This works, but doesn't look great
    // Keep for now, but if I have time come back and optimize this
    private var databaseHolder = FirebaseDatabase.getInstance()

    /**
     * Physically deletes the user's data in my Realtime Database, both in the *Users* node and
     * the *Plants Found* node. Also deletes the user's account in the Authentication tab.
     *
     * When the user deletes their account they are prompted to reenter their login data, and I
     * re-authenticate them using their login information here.
     *
     * **MOVE THE RE-AUTHENTICATION TO [DataRepository]!!**
     *
     * @see [DataRepository.deleteUserFromDB]
     * @see [DataRepository.deleteUserAuth]
     * @param email [String]
     * @param password: [String]
     * @param user [FirebaseUser]
     *
     * @author Tylor J. Hanshaw
     */
    fun deleteUserAccount(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DataRepository.reAuthenticateUser(email, password)
            //logout()
        }
    }

    /**
     * Called from [MapsActivity][com.example.forager.MapsActivity] when the user logs out. Clears
     * all map markers added in case someone different logs on next.
     *
     * @see clearMarkers
     * @see [DataRepository.clearOldListData]
     * @see [DataRepository.signOut]
     *
     * @author Tylor J. Hanshaw
     */
    fun logout() {
        DataRepository.clearOldListData()
        clearMarkers()
        DataRepository.signOut()
    }


                    /* Handles getting data when first logging in via Coroutines */
    /**********************************************************************************************/

    /**
     * Using coroutines to retrieve the user's *personal found plant list* in my
     * Realtime Database. I get the data from [DataRepository] inside of a
     * *LiveData coroutine scope* block using the [Dispatchers.IO] dispatcher and
     * emit the data received to any observers.
     *
     * @see [DataRepository.getResponseFromDB]
     *
     * @author Tylor J. Hanshaw
     */
    val observeFoundPlantList = liveData(Dispatchers.IO) {
        emit(DataRepository.getResponseFromDB())
    }


    /**
     * Using coroutines to retrieve the user's information in my
     * Realtime Database. I get the data from [DataRepository] inside of a
     * *LiveData coroutine scope* block using the [Dispatchers.IO] dispatcher and
     * emit the data received to any observers.
     *
     * @see [DataRepository.getUserInfo]
     *
     * @author Tylor J. Hanshaw
     */
    val observeUserInfo = liveData(Dispatchers.IO) {
        emit(DataRepository.getUserInfo())
    }


                                /* Handling the toggling of markers */
    /**********************************************************************************************/

    // This gets the user's list of plants found and re-creates the markers on the map
    private val plantsFoundMarkers: MutableList<Marker> = mutableListOf()
    val markerSize get() = plantsFoundMarkers.size // DEBUGGING

    /**
     * Caches a [Marker] locally when a user finds a new plant. Allows the user to toggle
     * on or off all [Marker]'s on the map.
     *
     * @see [com.example.forager.fragments.MapsFragment.getResponseUsingCoroutine]
     * @see plantsFoundMarkers
     * @param marker [Marker]
     */
    fun addNewMarker(marker: Marker) {
        plantsFoundMarkers.add(marker)
    }

    /**
     * Removes a marker from the cache when a user removes a plant of their *personal plant list*.
     * I'm both removing the marker from the map and from the cached list of [Marker]'s.
     *
     * @see [com.example.forager.fragments.PersonalPlantListFragment.PersonalPlantListAdapter]
     * @see plantsFoundMarkers
     * @param lat [Double]
     * @param long [Double]
     */
    fun removeMarker(lat: Double, long: Double) {
        val markerIndex = findMarker(LatLng(lat, long))
        if(markerIndex != -1) {
            plantsFoundMarkers[markerIndex].remove()
            plantsFoundMarkers.removeAt(markerIndex)
        }
    }

    /**
     * Utility function used to find the correct [Marker] to remove from the
     * map and the cached list of markers.
     *
     * *Returns -1 if the marker is not found.*
     *
     * @see [plantsFoundMarkers]
     * @param coords [LatLng]
     * @return [Int]
     *
     * @author Tylor J. Hanshaw
     */
    private fun findMarker(coords: LatLng): Int {
        for(m in plantsFoundMarkers.indices) {
            if(plantsFoundMarkers[m].position.latitude == coords.latitude &&
                plantsFoundMarkers[m].position.longitude == coords.longitude)
                    return m
        }
        return -1
    }

    fun getMarker(marker: Marker?): Marker {
        val markerIndex = findMarker(LatLng(marker!!.position.latitude, marker.position.longitude))
        return plantsFoundMarkers[markerIndex]
    }

    /**
     * Utility function used for clearing the cached list of [Marker]'s.
     *
     * @see [plantsFoundMarkers]
     */
    private fun clearMarkers() { plantsFoundMarkers.clear() }

    /**
     * Used when toggling the [Marker]'s on the map on or off.
     *
     * @see [com.example.forager.fragments.MapsFragment]
     * @param toggled [Boolean]
     */
    fun toggleMarkers(toggled: Boolean) {
        var count = 0
        plantsFoundMarkers.forEach { marker ->
            Log.d(LOG, "Marker #${count++}: $marker")
            marker.isVisible = toggled
        }
    }


                                        /* Utility functions */
    /**********************************************************************************************/

    /**
     * Utility function that grabs the current date to timestamp new plants the user finds.
     * Date is in the *MM-dd-yyyy* format.
     *
     * @return [String]
     *
     * @author Tylor J. Hanshaw
     */
    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("MM-dd-yyyy")
        return formatter.format(Calendar.getInstance().time)
    }

    /**
     * Finds a plant node given the plant's common name.
     *
     * @see [DataRepository.findPlantNode]
     * @param plantName [String]
     * @return [Plant]?
     */
    fun findPlantNode(plantName: String): Plant? = DataRepository.findPlantNode(plantName)

    /**
     * Utility function that checks the lengths of both the common and scientific
     * names of each plant in the fragment [PersonalPlantList][com.example.forager.fragments.PersonalPlantListFragment]'s
     * RecyclerView and the fragment [PlantdatabaseFragment][com.example.forager.fragments.PlantDatabaseFragment]'s
     * RecyclerView.
     *
     * This shortens the plant's scientific name so it will fit in each View comfortably.
     *
     * @see [com.example.forager.fragments.PersonalPlantListFragment.PersonalPlantListHolder.bindView]
     * @see [com.example.forager.fragments.PlantDatabaseFragment.PlantDBHolder.bindView]
     * @see [Plant]
     * @param plant [Plant]
     * @return [String]
     *
     * @author Tylor J. Hanshaw
     */
    fun checkNameLengths(plant: Plant): String {
        if(plant.commonName.length > 19 && plant.scientificName.length > 20) {
            var shortenedScientificName = ""
            for(letter in 0..10) shortenedScientificName += plant.scientificName[letter]
            return "$shortenedScientificName..."
        }
        return plant.scientificName
    }

    /**
     * Utility function that translates the [Plant]'s type from an integer value to
     * its corresponding string value.
     *
     * @param plantType [Int]
     * @return Plant's type
     *
     * @author Tylor J. Hanshaw
     */
    fun getPlantType(plantType: Int): String {
        return when(plantType) {
            0 -> "Wildflower"
            1 -> "Fern"
            2 -> "Tree/Shrub/Vine"
            3 -> "Grasses/Sedges/Rushes"
            else -> "Unknown"
        }
    }

    /**
     * Utility that converts the photo taken by the user from a *bitmap* to a file. This file is
     * to be uploaded to Firebase Cloud Storage.
     *
     * @param takenImage [Bitmap]
     * @param fileNameToSave [String]
     * @return Returns the image in [File]? format.
     */
    fun convertBitmapToFile(takenImage: Bitmap, fileDir: File): File? {
        var file: File? = null
        val userUid = FirebaseAuth.getInstance().currentUser!!.uid
        return try {
            file = File(
                fileDir,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".jpeg")
            file.createNewFile()

            // Converting Bitmap to byte array
            val bos = ByteArrayOutputStream()
            takenImage.compress(Bitmap.CompressFormat.JPEG, 0, bos)
            val photoData = bos.toByteArray()

            // Write the bytes to the file
            val fos = FileOutputStream(file)
            fos.write(photoData)
            fos.flush()
            fos.close()
            Log.d(LOG, "Bitmap successfully converted to a file")
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(LOG, "Bitmap unsuccessfully converted to a file")
            file
        }
    }
}

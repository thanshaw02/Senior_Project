/**
 * - LiveData here that gets data from the DB
 * - Then feed that LiveData into my HomeViewModel
 */

package com.example.forager.repository

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.forager.localdata.PlantsDatabaseHelper
import com.example.forager.localdata.model.Plant
import com.example.forager.remotedata.*
import com.example.forager.remotedata.model.PlantListNode
import com.example.forager.remotedata.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.lang.Exception
import android.database.CursorWindow
import java.lang.reflect.Field

interface MyCallback {
    fun getDataFromDB(data: Any?) // Takes a parameter of type "Any?", allowing me to use this for almost anything
}

private const val LOG = "DataRepository:"

/**
 * Repository singleton that communicates with both my *Realtime Database* and my *local database*.
 *
 * @author Tylor J. Hanshaw
 */
// This is where I talk to my Realtime Database, authy, and my SQLite database
object DataRepository {


    // Remote data source
    private var remoteDataSource = FirebaseDatabase.getInstance()

                                        /* LOCAL DATA */
    /**********************************************************************************************/

    // If I do end up storing the local plants in a MutableList, store it in a ViewModel!!!!!!
    // List and getter function for the local database - holds all of the plants that are needed without internet
    // May want to remove this! Possibly just grab data straight from the database rather than storing in an intermediate MutableList
    private val localPlantData: MutableList<Plant> = mutableListOf()
    val getLocalPlantData get() = localPlantData

    // This is for my auto-guesser in my AutoCompleteTextView
    private val plantCommonNames: MutableList<String> = mutableListOf()
    val getPlantCommonName get() = plantCommonNames

    // Added the first val here to try to transition to a MVVM paradigm
    fun getLocalPlantData(context: Context): MutableList<Plant> {
        // val localPlantData: MutableList<Plant> = mutableListOf() // this is new, and so is the return type for this function
        val ld = PlantsDatabaseHelper(context).readableDatabase
        val cursor = ld.rawQuery("SELECT * FROM Plants", null)
        while(cursor.moveToNext()) {
//            val photoAsBitmap = BitmapFactory.decodeByteArray(
//                cursor.getBlobOrNull(cursor.getColumnIndexOrThrow("Plant Photo")),
//                0,
//                cursor.getBlobOrNull(cursor.getColumnIndexOrThrow("Plant Photo"))!!.size
//            )
            localPlantData.add(
                Plant(
                    cursor.getString(cursor.getColumnIndexOrThrow("Common Name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Scientific Name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("Plant Type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Color")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Sun")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Height")),
//                    photoAsBitmap
                ))
            plantCommonNames.add(cursor.getString(cursor.getColumnIndexOrThrow("Common Name")))
        }
        cursor.close()
        return localPlantData
    }

    // Find a given Plant node given a common name
    fun findPlantNode(commonName: String): Plant? {
        return localPlantData.find { plantNode ->
            plantNode.commonName == commonName
        }
    }

                                            /* REALTIME DATA */
    /**********************************************************************************************/

    // Initializing database references, FirebaseAuth
    private val firebaseAuth = Firebase.auth // Auth used for FirebaseAuth operations and RealtimeDatabase
    private var plantsFoundDBRef = remoteDataSource.getReference("Plants_Found")
    private var userDBRef = remoteDataSource.getReference("Users")

    // Use this variable to upload the user's photo taken of their plant
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val plantImageStorageRef = firebaseStorage.child("plant_photos")


    suspend fun addPlantPhotoToCloudStorage(photo: File?, plantUid: String) {
        withContext(Dispatchers.IO) {
            val userStorageRef = plantImageStorageRef.child(firebaseAuth.currentUser!!.uid).child(plantUid)
            val photoUri = Uri.fromFile(photo)
            Log.d(LOG, "Photo to adds URI: $photoUri")
            userStorageRef.putFile(photoUri).addOnCompleteListener { result ->
                if(result.isSuccessful) Log.d(LOG, "Photo file URU successfully added to Cloud Storage")
                else Log.d(LOG, "Photo file URI unsuccessfully added to Cloud Storage")
            }.await()
            Log.d(LOG, "Download URL: ${userStorageRef.downloadUrl.await()}")
        }
    }

    suspend fun deletePlantPhotoFromCloudStorage(plantPhotoUrl: String) {
        withContext(Dispatchers.IO) {
            firebaseStorage.child(plantPhotoUrl).delete()
                .addOnCompleteListener {
                    if(it.isSuccessful) Log.d(LOG, "Photo was successfully removed.")
                    else Log.d(LOG, "Photo was not removed.")
                }
        }
    }

    // Figured using a callback function here would be easier than coroutines??
    fun getPlantPhotoFromCloudStorage(plantPhotoUri: String, callback: MyCallback) {
        val localFile = File.createTempFile("tempFileImage", "jpeg")
        firebaseStorage.child(plantPhotoUri).getFile(localFile).addOnCompleteListener {
            if(it.isSuccessful) {
                Log.d(LOG, "Successfully retrieved plant photo: ${it.result}")
                val photoAsBitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                callback.getDataFromDB(photoAsBitmap) // Converting the photo Uri to a file here
            }
            else Log.d(LOG, "Photo was not retrieved.")
        }
    }

    // Grabbing the user's found plant list using a callback
    // Not currently using this, and probably won't in the future
    // But this was be trying to fix a bug
    // The bug is that if you add a plant before opening your personal list, the plant added won't be seen until you re-log
    // The issue is happening because the observer that observes new nodes added isn't set up until you open your list
    // So if you open your list and then add plants you will see those added, but if you don't open your list they won't show up
    fun getUsersPreviousPlantsFoundList(callback: MyCallback) {
        plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).get().addOnCompleteListener {
            val response = DataResponse()
            if(it.isSuccessful) {
                val result = it.result
                response.dataSnapshot = result
            }
            else {
                response.exception = it.exception
            }
            callback.getDataFromDB(response)
        }
    }

    // Coroutine solution to getting the user's found plants list when logging in
    // This uses my "DataResponse" data class to either get the user's found plants list
    // Or to store the error if the operation was unsuccessful
    suspend fun getResponseFromDB(): DataResponse {
        val dataResponse = DataResponse()
        try {
            dataResponse.plants = plantsFoundDBRef
                .child(firebaseAuth.currentUser!!.uid)
                .get().await().children.map { snapShot ->
                    val node = PlantListNode(
                        snapShot.child("lat").value.toString().toDouble(),
                        snapShot.child("long").value.toString().toDouble(),
                        Plant(
                            snapShot.child("plantAdded").child("commonName").value
                                .toString(),
                            snapShot.child("plantAdded").child("scientificName").value.
                            toString(),
                            snapShot.child("plantAdded").child("plantType").value
                                .toString().toInt(),
                            snapShot.child("plantAdded").child("plantColor").value
                                .toString(),
                            snapShot.child("plantAdded").child("sun").value
                                .toString(),
                            snapShot.child("plantAdded").child("height").value
                                .toString()
                        ),
                        snapShot.child("plantNotes").value.toString(),
                        snapShot.child("dateFound").value.toString(),
                        snapShot.child("plantPhotoUri").value.toString()
                    )
                    node.setUID(snapShot.key) // Instead of auto-generating a uid, I just need to uid given to the plant node previously
                    node
            }.toMutableList()
        } catch (ex: Exception) {
            dataResponse.exception = ex
        }
        return dataResponse
    }

    // This will clear the markers from whoever was logged in previously
    fun clearOldListData() {
        plantCommonNames.clear()
        localPlantData.clear()
    }

    /**
     *                          GETTING USER'S DATA
     */

    // Getting user's info but with coroutines
    suspend fun getUserInfo(): UserResponse {
        val userResponse = UserResponse()
        try {
            userResponse.user = userDBRef
                .child(firebaseAuth.currentUser!!.uid)
                .get().await().getValue(User::class.java)
        } catch (ex: Exception) {
            userResponse.exception = ex
        }
        return userResponse
    }

    fun getUsersFullName(callback: MyCallback) {
        userDBRef.child(firebaseAuth.currentUser!!.uid).child("fullName").get()
            .addOnCompleteListener {
                val response = StringDataResponse()
                if(it.isSuccessful) {
                    val result = it.result
                    response.data = result.value.toString()
                    callback.getDataFromDB(response)
                }
                else {
                    response.exception = it.exception
                    callback.getDataFromDB(response)
                }
            }
    }

    /**
     *                          GETTING USER'S PLANTS FOUND COUNT
     */

    fun getNumberOfPlantsFound(callback: MyCallback) {
        userDBRef.child(firebaseAuth.currentUser!!.uid)
            .child("numPlantsFound")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val number = snapshot.value
                    callback.getDataFromDB(number.toString())
                }

                override fun onCancelled(error: DatabaseError) { }
            })
    }

    // This physically adds the new plant to the database
    // Rename it to match my remove method??
    fun addPlantLocation(plantListNode: PlantListNode) {
        plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).child(plantListNode.getUID())
            .setValue(plantListNode)
    }

    fun removePlantFromDB(uid: String) {
        plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).child(uid).removeValue().addOnCompleteListener {
            if(it.isSuccessful) Log.d(LOG, "Removed plant successfully! Plant uid was: $uid")
            else Log.d(LOG, "Was not successful in removing the plant with the uid of: $uid")
        }
    }

    suspend fun incrementPlantsFound(increment: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").setValue(increment).await()
        }
    }

    suspend fun decrementPlantsFound(decrement: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").setValue(decrement).await()
        }
    }


//                                              LOGIN/REGISTER CODE BELOW
//    /***************************************************************************************************************************/

    fun signOut() { firebaseAuth.signOut() }

    suspend fun singInWithEmail(act: Activity, email: String, password: String) =
        withContext(Dispatchers.IO) {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(act) { task ->
                    if(task.isSuccessful) {
                        // Updates the UI here
                        // Moves the user to the map portion of the app with their account information
                        Log.d(LOG, "signedInWithEmail:success")
                        val user = firebaseAuth.currentUser
                        //goToHomeScreen(user!!)
//                    homeVM.getUserInfoAsync()
                    }
                    else {
                        // Sign in failed, entered wrong information
                        Log.w(LOG, "signedInWithEmail:failure", task.exception)
                    }
                }.await()
        }

    // Trying to remove the use info all in one suspend function here
    suspend fun reAuthenticateUser(email: String, password: String) {
        withContext(Dispatchers.IO) {
            firebaseAuth.currentUser!!.reauthenticate(EmailAuthProvider.getCredential(email, password))
                .addOnCompleteListener {
                    if(it.isSuccessful) {
                        Log.d(LOG, "Successfully delete the user's auth account.")
                        deleteUserAuth()
                        signOut()
                    }
                    else Log.d(LOG, "Failed to re-authenticate user.")
                }
        }
    }

    private fun deleteUserAuth() {
        firebaseAuth.currentUser!!.delete().addOnCompleteListener {
            if(it.isSuccessful) Log.d(LOG, "Fully deleted the user's profile.")
            else Log.d(LOG, "Failed to delete user's auth.")
        }
    }
}
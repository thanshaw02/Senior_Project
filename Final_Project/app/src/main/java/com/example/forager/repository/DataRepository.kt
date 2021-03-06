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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface MyCallback {
    fun getDataFromDB(data: Any?) // Takes a parameter of type "Any?", allowing me to use this for almost anything
}

private const val LOG = "DataRepository:"

/**
 * Repository singleton that communicates with both my *Realtime Database* and my *local database*.
 *
 * @author Tylor J. Hanshaw
 */
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
        while (cursor.moveToNext()) {
            localPlantData.add(
                Plant(
                    cursor.getString(cursor.getColumnIndexOrThrow("Common Name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Scientific Name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("Plant Type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Color")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Sun")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Height")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Photo URL")),
                )
            )
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
    private val firebaseAuth =
        Firebase.auth // Auth used for FirebaseAuth operations and RealtimeDatabase
    private var plantsFoundDBRef = remoteDataSource.getReference("Plants_Found")
    private var userDBRef = remoteDataSource.getReference("Users")

    // Use this variable to upload the user's photo taken of their plant
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val personalPlantImageStorageRef = firebaseStorage.child("plant_photos")
    private val localPlantImageStorageRef = firebaseStorage.child("local_plant_photos")

    private val plantAddedToDB: MutableLiveData<PlantListNode> = MutableLiveData()
    val getPlantAddedToDB: LiveData<PlantListNode> get() = plantAddedToDB


    // Uploading the photo taken by the user to Firebase Storage and if successful attempting to
    // download the URL of that uploaded photo.
    // If downloading the URL is successful then I'm adding that URL to the PlantListNode instance
    // and adding that to the Realtime Database with the URL linking that node to the uploaded photo.
    suspend fun addPlantPhotoToCloudStorage(photo: File?, nodeToAdd: PlantListNode) {
        withContext(Dispatchers.IO) {
            val userStorageRef = personalPlantImageStorageRef
                .child(firebaseAuth.currentUser!!.uid).child(nodeToAdd.getUID())
            val photoUri = Uri.fromFile(photo)
            userStorageRef.putFile(photoUri).addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    userStorageRef.downloadUrl.addOnCompleteListener {
                        if (it.isSuccessful) {
                            val photoUrl = it.result
                            nodeToAdd.setPlantPhotoUriNode(photoUrl.toString())
                            plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid)
                                .child(nodeToAdd.getUID())
                                .setValue(nodeToAdd).addOnCompleteListener {
                                    if (it.isSuccessful) plantAddedToDB.value = nodeToAdd
                                    else Log.e(LOG, "Something went wrong.. horribly wrong.")
                                }
                        }
                    }
                } else Log.e(LOG, "Photo file URI unsuccessfully added to Cloud Storage")
            }.await()
        }
    }

    // Physically deleting the photo associated with the removed plant from Firebase Storage
    suspend fun deletePlantPhotoFromCloudStorage(plantPhotoUid: String) {
        withContext(Dispatchers.IO) {
            personalPlantImageStorageRef.child(firebaseAuth.currentUser!!.uid)
                .child(plantPhotoUid).delete().addOnCompleteListener {
                    if (it.isSuccessful) Log.d(LOG, "Photo was successfully removed.")
                    else Log.e(LOG, "Photo was not removed.")
                }
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
                            snapShot.child("plantAdded").child("scientificName").value
                                .toString(),
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
                    node.setUID(snapShot.key)
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

    /**
     *                          GETTING USER'S PLANTS FOUND COUNT
     */

    fun getNumberOfPlantsFound(callback: MyCallback) {
        userDBRef.child(firebaseAuth.currentUser!!.uid)
            .child("numPlantsFound")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val number = snapshot.value
                    callback.getDataFromDB(number.toString())
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // This physically adds the new plant to the database
    // Rename it to match my remove method??
    fun addPlantLocation(plantListNode: PlantListNode) {
        plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).child(plantListNode.getUID())
            .setValue(plantListNode)
    }

    suspend fun removePlantFromDB(uid: String) {
        withContext(Dispatchers.IO) {
            plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).child(uid).removeValue()
                .addOnCompleteListener {
                    if (it.isSuccessful) Log.d(
                        LOG,
                        "Removed plant successfully! Plant uid was: $uid"
                    )
                    else Log.d(
                        LOG,
                        "Was not successful in removing the plant with the uid of: $uid"
                    )
                }
        }
    }

    suspend fun incrementPlantsFound(increment: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound")
                .setValue(increment).await()
        }
    }

    suspend fun decrementPlantsFound(decrement: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound")
                .setValue(decrement).await()
        }
    }


//                                              LOGIN/REGISTER CODE BELOW
//    /********************************************************************************************/

    fun signOut() {
        firebaseAuth.signOut()
    }

    private val observeUsernameChangesRepo: MutableLiveData<String> = MutableLiveData()
    val getObservedUsernameChangesRepo: LiveData<String> get() = observeUsernameChangesRepo


    suspend fun reAuthenticateUserForUpdates(
        currentEmail: String,
        currentPassword: String,
        newUsername: String,
        newEmail: String,
        newPassword: String
    ) {
        withContext(Dispatchers.IO) {
            firebaseAuth.currentUser!!.reauthenticate(
                EmailAuthProvider.getCredential(
                    currentEmail,
                    currentPassword
                )
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    observeUsernameChangesRepo.value = newUsername
                    userDBRef.child(firebaseAuth.currentUser!!.uid).child("userName")
                        .setValue(newUsername).addOnSuccessListener {
                            Log.d(LOG, "Successfully updated users username.")
                        }
                    firebaseAuth.currentUser!!.updateEmail(newEmail).addOnSuccessListener {
                        Log.d(LOG, "Successfully updated user's email.")
                    }
                    firebaseAuth.currentUser!!.updatePassword(newPassword).addOnSuccessListener {
                        Log.d(LOG, "Successfully updated the user's password.")
                    }
                }
            }
        }
    }

    // Trying to remove the use info all in one suspend function here
    // I'm using a "Firebase Function" here in the server side, what it does it detects when a user's
    // Authentication credentials are deleted, if that happens all data associated with that user
    // (I specify this in the Firebase Console) are removed. This can include data in Firebase Storage,
    // photos of plants in my case, and my Realtime Database.
    suspend fun reAuthenticateUserForDeletion(email: String, password: String) {
        withContext(Dispatchers.IO) {
            firebaseAuth.currentUser!!.reauthenticate(
                EmailAuthProvider.getCredential(
                    email,
                    password
                )
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(LOG, "Successfully delete the user's auth account.")
                    deleteUserAuth()
                    signOut()
                } else Log.d(LOG, "Failed to re-authenticate user.")
            }
        }
    }

    private fun deleteUserAuth() {
        firebaseAuth.currentUser!!.delete().addOnCompleteListener {
            if (it.isSuccessful) Log.d(LOG, "Fully deleted the user's profile.")
            else Log.d(LOG, "Failed to delete user's auth.")
        }
    }
}
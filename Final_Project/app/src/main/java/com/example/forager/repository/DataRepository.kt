/**
 * - LiveData here that gets data from the DB
 * - Then feed that LiveData into my HomeViewModel
 */

package com.example.forager.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.forager.localdata.PlantsDatabaseHelper
import com.example.forager.localdata.model.Plant
import com.example.forager.remotedata.PlantListNode
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

interface MyCallback {
    fun onCallback(plantsFound: String)
}

private const val LOG = "DataRepository:"

// This is where I talk to my Realtime Database, authy, and my SQLite database
object DataRepository {


    // Remote data source
    private var remoteDataSource = FirebaseDatabase.getInstance()

                                                    /* LOCAL DATA */

    // Probably a really BAD way of attacking this issue "getLocalPlantData" ONLY ONCE
    var localDatabaseMade = true

    // If I do end up storing the local plants in a MutableList, store it in a ViewModel!!!!!!
    // List and getter function for the local database - holds all of the plants that are needed without internet
    // May want to remove this! Possibly just grab data straight from the database rather than storing in an intermediate MutableList
    private val localPlantData: MutableList<Plant> = mutableListOf()
    val getLocalPlantData get() = localPlantData

    // If I do end up storing the plant names in a MutableList, store it in a ViewModel!!!!!!
    // List and getter function that holds all plant common names in my database
    // This is for my auto-guesser in my AutoCompleteTextView
    private val plantCommonNames: MutableList<String> = mutableListOf()
    val getPlantCommonName get() = plantCommonNames

    // Added the first val here to try to transition to a MVVM paradigm
    fun getLocalPlantData(context: Context): MutableList<Plant> {
        var count = 0
        // val localPlantData: MutableList<Plant> = mutableListOf() // this is new, and so is the return type for this function
        val ld = PlantsDatabaseHelper(context).readableDatabase
        val cursor = ld.rawQuery("SELECT * FROM Plants", null)
        while(cursor.moveToNext()) {
            localPlantData.add(
                Plant(
                    cursor.getString(cursor.getColumnIndexOrThrow("Common Name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Scientific Name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("Plant Type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Color")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Sun")),
                    cursor.getString(cursor.getColumnIndexOrThrow("Height")),
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

    private val firebaseAuth = Firebase.auth

    // Database references etc.
    private var plantsFoundDBRef = remoteDataSource.getReference("Plants Found")
    var userDBRef = remoteDataSource.getReference("Users")

    // This does work! This is the only set of methods that are able to gte the whole Personal Plant List for each user
    // These two LiveData's are for the lists
    private val personalPlantListOfUser: MutableLiveData<MutableList<DataSnapshot>> = MutableLiveData()
    val getPersonalPlantListOfUser: LiveData<MutableList<DataSnapshot>> get() = personalPlantListOfUser
    fun getPersonalPlantListOfUser() { // This function is ONLY called for my plant lists fragments/activity
        plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).get().addOnCompleteListener { list ->
            if(list.isSuccessful) {
                val ds = list.result
                val tempList: MutableList<DataSnapshot> = mutableListOf()
                for(snapshot in ds.children) {
                    tempList.add(snapshot)
                }
                personalPlantListOfUser.value = tempList
            }
        }
    }

    // This will clear the markers from whoever was logged in previously
    fun clearOldListData() {
        personalPlantListOfUser.value!!.clear()

    }

    /**
     *                          GETTING USER'S DATA
     */
    // These 7 lines load the logged in user's data when they first log in
    // ONLY HAPPENS ONCE
    private val user: MutableLiveData<DataSnapshot> = MutableLiveData()
    val getUser: LiveData<DataSnapshot> get() = user
    fun getTheUserFromFirebase() {
        userDBRef.child(firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { userDS ->
            user.value = userDS
        }
    }

    fun getUsersFullName(): String = user.value!!.child("fullName").value.toString()

    /**
     *                          GETTING USER'S PLANTS FOUND COUNT
     */

    private val testingNumPlantsFoundGet: MutableLiveData<DataSnapshot> = MutableLiveData()
    val getTestingNumPlantsFoundGet: LiveData<DataSnapshot> get() = testingNumPlantsFoundGet
    fun getUsersPlantFoundData() {
        userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").get().addOnCompleteListener {
            if(it.isSuccessful) {
                testingNumPlantsFoundGet.postValue(it.result)
                Log.d(LOG, "Was able to get the user's \"numPlantsFound\" data: ${testingNumPlantsFoundGet.value}")
            }
            else Log.d(LOG, "Could not get numPlantsFound from the user.. LINE 228")
        }
    }




    // As of now, I don't think this try {...} catch {...} is actually doing anything
    //      - Possibly scrap it or try getting it working?
    //      - I'll probably tru to keep it since it's probably good practice to use a try {...} catch {...} block
    // Function that physically adds the new plant that a user found to the users personal "found plant" list
    fun addPlantLocation(coord: LatLng, plantAdding: Plant, plantNotes: String) { /* I could possibly use LiveData here actually? */
        try {
            val plantUUID = UUID.randomUUID()
            val formatter = SimpleDateFormat("MM-dd-yyyy")
            val formattedDate = formatter.format(Calendar.getInstance().time)
            plantsFoundDBRef.child(firebaseAuth.currentUser!!.uid).child("$plantUUID")
                .setValue(
                    PlantListNode(
                        coord.latitude,
                        coord.longitude,
                        plantAdding,
                        plantNotes,
                        formattedDate))
        } catch(e: FirebaseException) {
            Log.e(LOG, "Failed to add plant with error: $e")
        }
    }

    suspend fun incrementPlantsFound(increment: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").setValue(increment).await()
        }
    }

    fun readData(callback: MyCallback) {
        userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val number = snapshot.value
                callback.onCallback(number.toString())
            }

            override fun onCancelled(error: DatabaseError) { }
        })
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
    fun deleteUserAuth(user: FirebaseUser) {
        Log.d(LOG, "Users UID: ${user.uid}")
        user.delete().addOnCompleteListener {
            if(it.isSuccessful) Log.d(LOG, "User was successfully deleted.")
            else Log.d(LOG, "User was NOT successfully deleted.")
        }
    }

    // This is a test to see if this block of code (which is used in my coroutine method above) works on its own
    // This does in fact work on its own, so the problem is with how I'm handling coroutines
    fun deleteUserFromDB(user: FirebaseUser, dbRef: DatabaseReference) {
        Log.d(LOG, "User UID: ${user.uid}")
        val uid = user.uid
        dbRef.child("Users").child(user.uid).removeValue().addOnCompleteListener {
            if(it.isSuccessful) Log.d(LOG, "User has been deleted from database.")
            else Log.d(LOG, "User has NOT been deleted from database.")
        }
        dbRef.child("Plants Found").child(user.uid).removeValue().addOnCompleteListener {
            if(it.isSuccessful) Log.d(LOG, "Users personal plant list has been deleted from the database.")
            else Log.d(LOG, "Users personal plant list has NOT been deleted from the database.")
        }
    }













//    private val someString: String = ""
//    val getSomeString get() = someString
    suspend fun getUsersPersonalPlantCount(): DataSnapshot? {
        val something = userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").get().await()
        Log.d(LOG, "This is from DataRepository: $something")
        Log.d(LOG, "This did not work")
        return something
    }

    suspend fun updateUsersPlantsFound(update: Int) {
        withContext(Dispatchers.IO) {
            userDBRef.child(firebaseAuth.currentUser!!.uid).child("numPlantsFound").setValue(update).await()
        }
    }



}
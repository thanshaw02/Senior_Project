package com.example.forager.activities.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy.LOG
import com.example.forager.activities.MapsActivity
import com.example.forager.remotedata.model.User
import com.example.forager.databinding.ActivityRegisterBinding
import com.example.forager.activities.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.util.*

/******************************************************************************************************************************************************
 *  TODO: Change the stroke colo of the input boxes (looks pretty weird still)                                                                        *
*******************************************************************************************************************************************************/

private const val TAG = "RegisterActivity:"

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var newUser: User

    // Register fields
    private lateinit var fullNameET: EditText
    private lateinit var usernameET: EditText
    private lateinit var emailET: EditText
    private lateinit var passwordET: EditText

    //private val dataRepo by lazy { DataRepository() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initializing fields
        fullNameET = binding.editTextFullName
        usernameET = binding.editTextUsername
        emailET = binding.editTextEmailAddress
        passwordET = binding.editTextPassword

        // Initializing database
        database = Firebase.database.reference

        auth = Firebase.auth

        // Check to see if the password is at least 6 characters long!!
        // TODO: Right now the user can register and go to the home screen without entering valid register data
        binding.registerBtn.setOnClickListener {
            if((fullNameET.text.isNotEmpty() && usernameET.text.isNotEmpty()) && (emailET.text.isNotEmpty() && passwordET.text.isNotEmpty())) {
                registerNewUser(usernameET.text.toString(), fullNameET.text.toString(), emailET.text.toString(), passwordET.text.toString())
            }
            else Toast.makeText(this, "Please fill out all of the fields.", Toast.LENGTH_SHORT).show()
        }

        // Need to close this activity, otherwise it will still be running in the background until the app is fully closed
        binding.backToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            // try to close this activity for good, maybe??
        }
    }

    // Come back to this, try to consolidate code into one place instead of throwing it everywhere
    private fun goToHomeScreen() {
        val intent = MapsActivity.newInstance(this, 1)
        startActivity(intent)
    }

    // If for whatever reason the user is on this activity and is logged in, this will bring them to the home page
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null) {
            goToHomeScreen()
        }
    }

    private fun registerNewUser(username: String, fullName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    // User was created successfully and also has been signed in
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val updateProfile = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName).build()
                    user!!.updateProfile(updateProfile).addOnCompleteListener {
                        if(it.isSuccessful) {
                            Log.d(TAG, "User has been authenticated and full name updated.")
                            writeNewUser(user.uid, username)
                            goToHomeScreen()
                        }
                    }
                }
                else {
                    // If user account could not be created
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    //goToHomeScreen(null)
                }
            }
    }

    private fun writeNewUser(userID: String, username: String) {
        // Getting the current date to write to my Realtime Database
        val currentDate = Calendar.getInstance().time
        val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(currentDate)
        newUser = User(username, formattedDate)
        database.child("Users").child(userID).setValue(newUser)
    }
}

/*
private fun registerNewUser(username: String, firstName: String, email: String, password: String) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if(task.isSuccessful) {
                // User was created successfully and also has been signed in
                Log.d(TAG, "createUserWithEmail:success")
                val user = auth.currentUser
                writeNewUser(user!!.uid, username, firstName, email)
                goToHomeScreen(user)
                finish()
            }
            else {
                // If user account could not be created
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                //goToHomeScreen(null)
            }
        }
}

private fun writeNewUser(userID: String, username: String, firstName: String, email: String) {
    // Getting the current date to write to my Realtime Database
    val currentDate = Calendar.getInstance().time
    val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(currentDate)
    newUser = User(username, firstName, email, formattedDate)
    database.child("Users").child(userID).setValue(newUser)
}
*/
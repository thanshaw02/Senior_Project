package com.example.forager.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.forager.MapsActivity
import com.example.forager.R
import com.example.forager.databinding.ActivityLoginBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Event.LOGIN
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val TAG = "LoginActivity"
const val INTENT_MESSAGE = "com.example.forager.SUCCESSFUL_LOGIN"

class LoginActivity : AppCompatActivity() {

    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth

    // Firebase analytics instance
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        firebaseAnalytics = Firebase.analytics

        binding.loginBtn.setOnClickListener {
            if(binding.editTextTextEmailAddress.text.toString() != "" && binding.editTextTextPassword.text.toString() != "") {
                signIn(binding.editTextTextEmailAddress.text.toString(), binding.editTextTextPassword.text.toString(), savedInstanceState)
            }
        }

    }

    // Attempts to login the user with the given information
    private fun signIn(email: String, password: String, bundle: Bundle?) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    // Updates the UI here
                    // Moves the user to the map portion of the app with their account information
                    firebaseAnalytics.logEvent(LOGIN, bundle)
                    val user = auth.currentUser
                    goToHomeScreen(user!!)
                    Log.d(TAG, "signedInWithEmail:success")
                }
                else {
                    // Sign in failed, entered wrong information
                    Log.w(TAG, "signedInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHomeScreen(user: FirebaseUser) {
        val intent = Intent(this, MapsActivity::class.java).apply {
            putExtra(INTENT_MESSAGE, user)
        }
        Log.d(TAG, "User metadata: ${user.metadata}")
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        // Checking to see if user has signed in or not (if a user has signed in then 'current' user will be non-null)
        val currentUser = auth.currentUser
        if(currentUser != null) {
            // Then change to a different view, which will probably be the maps view
        }
    }
}
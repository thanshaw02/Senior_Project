package com.example.forager.activities.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.airbnb.lottie.LottieAnimationView
import com.example.forager.activities.MapsActivity
import com.example.forager.R
import com.example.forager.databinding.ActivityLoginBinding
import com.example.forager.activities.register.RegisterActivity
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val LOG = "LoginActivity"
private const val RC_SIGN_IN = 9001

class LoginActivity : AppCompatActivity() {

    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth
    private var signInMethod: Int? = null

    // Firebase analytics instance
    // Used for logging app crashes and other stats like data usage, user retention, etc.
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivityLoginBinding

    // Come back to this, use these two variables instead of raw binding calls
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText

    // These variables were used for logging in using Google, the process is not hard but it's
    // very tedious and you have to deal with certain API keys you request from Google online.
    // This will be implemented in the future!
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity
    private var showOneTapUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initializing my FirebaseAuth variable
        auth = Firebase.auth
        firebaseAnalytics = Firebase.analytics

        emailText = binding.editTextEmailAddress
        passwordText = binding.editTextPassword

        binding.loginBtn.setOnClickListener {
            if (emailText.text.toString() != "" && passwordText.text.toString() != "") {
                signInWithEmail(
                    binding.editTextEmailAddress.text.toString(),
                    binding.editTextPassword.text.toString()
                )
            }
        }

        binding.createAccount.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        // This is a simple intent method, if I need to add extras make sure to add them as parameters and then add them to the intent
        fun newInstance(context: Context): Intent = Intent(context, LoginActivity::class.java)
    }

    // I would have preferred to use my DataRepository to do all of this work, but I would have needed
    // To move this activity and the Register activity to MapsActivity as fragments. This is due to
    // the fact that the ViewModel is tied to an Activities lifecycle, so when these activities are
    // destroyed all data used for them in my ViewModel is lost. Even if I were to use HomeViewModel.
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(LOG, "signedInWithEmail:success")
                    signInMethod = 1
                    goToHomeScreen()
                } else {
                    // Sign in failed, entered wrong information
                    Log.w(LOG, "signedInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHomeScreen() {
        val intent = MapsActivity.newInstance(this)
        startActivity(intent)
    }

    // Check if someone has already signed in with Google OR Facebook
    // Need to add the googleUser here to pass to the main map activity
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if ((currentUser != null)) {
            goToHomeScreen()
        }
    }
}
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
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivityLoginBinding

    // Two input fields
    // Come back to this, use these two variables instead of raw binding calls
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity
    private var showOneTapUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.google_web_client_id)) // My Google server client ID
                    .setFilterByAuthorizedAccounts(true) // Showing accounts that have signed in before
                    .build())
            .setAutoSelectEnabled(true)
            .build()

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

        // Come back to this, setting up Facebook login
        binding.loginFacebook.setOnClickListener {
            // This will not work, come back in a couple weeks
        }

        // Signing in via google auth
        binding.loginGoogle.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0, null)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(LOG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    // The user does not have a Google account registered with my app!
                    Log.d(LOG, e.localizedMessage!!)
                }
        }
    }

    companion object {
        // This is a simple intent method, if I need to add extras make sure to add them as parameters and then add them to the intent
        fun newInstance(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private fun loginInWithGoogle(idToken: String) {
        val credentials = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credentials)
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    Log.d(LOG, "User successfully logged in with Google!")
                    signInMethod = 2
                    goToHomeScreen()
                }
                else {
                    Log.d(LOG, "User was not signed in successfully with Google sign in.")
                    Toast.makeText(this, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithFacebook() {


    }

    // Attempts to login the user with the given information
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    when {
                        idToken != null -> {
                            // Using the ID token from signing in to sign the user into my app
                            loginInWithGoogle(idToken)
                            Log.d(LOG, "Got ID token.")
                        }
                        password != null -> {
                            // Not set up yet, but here if there is a password saved
                            Log.d(LOG, "Got password.")
                        }
                        else -> {
                            Log.d(LOG, "Something bad happened, no token!!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(LOG, "One-tap dialog was closed.")
                            showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(LOG, "One-tap encountered a network error.")
                        }
                        else -> {
                            Log.d(LOG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }
    }

    private fun goToHomeScreen() {
        val intent = MapsActivity.newInstance(this, signInMethod!!)
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
package com.example.forager.repository.login

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.example.forager.MapsActivity
import com.example.forager.R
import com.example.forager.databinding.ActivityLoginBinding
import com.example.forager.repository.register.RegisterActivity
import com.example.forager.viewmodel.HomeViewModel
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val LOG = "LoginActivity"

class LoginActivity : AppCompatActivity() {

    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth
    private lateinit var callBackManager: CallbackManager

    // Google sign-in variables
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient

    // Firebase analytics instance
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivityLoginBinding

    // Two input fields
    // Come back to this, use these two variables instead of raw binding calls
    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var loadingAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This is for my loading screen animation after logging in
        // Right now I need to figure out how to display then when retrieving data or working with data from my Realtime Database
        // loadingAnimationView = binding.loginLoading
        // loadingAnimationView.setAnimation(R.raw.loading_screen)
        // loadingAnimationView.playAnimation()

        // This is for singing in via Google
        // Not surrently working!
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initializing my FirebaseAuth variable
        auth = Firebase.auth
        firebaseAnalytics = Firebase.analytics

        emailText = binding.editTextEmailAddress
        passwordText = binding.editTextPassword

        binding.loginBtn.setOnClickListener {
            if(emailText.text.toString() != "" && passwordText.text.toString() != "") {
                signInWithEmail(binding.editTextEmailAddress.text.toString(), binding.editTextPassword.text.toString())
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
            // This will not work, come back in a couple weeks
        }
    }

    companion object {
        // This is a simple intent method, if I need to add extras make sure to add them as parameters and then add them to the intent
        fun newInstance(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private fun signInWithFacebook() {


    }

    // Attempts to login the user with the given information
    private fun signInWithEmail(email: String, password: String) {
//        val thisAct = this
//        lifecycleScope.launch(Dispatchers.IO) {
//            DataRepository.singInWithEmail(thisAct, email, password)
//            val user = FirebaseAuth.getInstance().currentUser
//            goToHomeScreen(user!!)
//        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    // Updates the UI here
                    // Moves the user to the map portion of the app with their account information
                    Log.d(LOG, "signedInWithEmail:success")
                    val user = auth.currentUser

                    goToHomeScreen()
                }
                else {
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
        if((currentUser != null)) {
            goToHomeScreen()
        }
    }

    override fun onPause() {
        Log.d(LOG, "onPause() called")
        super.onPause()
    }

    override fun onStop() {
        Log.d(LOG, "onStop() called")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(LOG, "onDestroy() called")
        super.onDestroy()
    }
}
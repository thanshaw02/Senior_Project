package com.example.forager.splashscreens

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.transition.Explode
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.viewModels
import com.airbnb.lottie.LottieAnimationView
import com.example.forager.MapsActivity
import com.example.forager.R
import com.example.forager.repository.login.LoginActivity
import com.example.forager.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity used a splash screen when the app is first opened.
 *
 * ** I want to have the apps data load while this is going!*
 *
 * @author Tylor J. Hanshaw
 */
private const val LOG = "SplashScreenActivity"

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var splashImage: LottieAnimationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        splashImage = findViewById(R.id.splash_animation)
        splashImage.animate().translationX(2500F).setDuration(1800).setStartDelay(1800)

        Handler().postDelayed({
//            FirebaseAuth.getInstance().currentUser!!.delete()
            if(FirebaseAuth.getInstance().currentUser != null) {
                Log.d(LOG, "There is a user logged in..")
                val intent = MapsActivity.newInstance(this)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            else {
                Log.d(LOG, "There is not a user logged in..")
                val intent = LoginActivity.newInstance(this)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }, 3000)
    }
}
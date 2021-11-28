package com.example.forager.splashscreens

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.airbnb.lottie.LottieAnimationView
import com.example.forager.R
import com.example.forager.viewmodel.HomeViewModel

class SplashScreenFragment : Fragment() {

    private lateinit var splashImage: LottieAnimationView

    private val homeVM by activityViewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_splash_screen, container, false)

        // window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        splashImage = view.findViewById(R.id.splash_animation)
        splashImage.animate().translationX(2500F).setDuration(1800).setStartDelay(1800)
//        Handler().postDelayed({
//            Log.d("SplashScreen", "Navigate to maps!")
//            navAPI!!.navigateToMap()
//            if(FirebaseAuth.getInstance().currentUser != null) {
//                val intent = MapsActivity.newInstance(requireContext())
//                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(requireActivity()).toBundle())
//            }
//            else {
//                Log.d("SplashScreen", "Navigate to maps!")
//
//                navAPI!!.navigateToMap()
//            }
//        }, 3000)

        return view
    }

    companion object {
        fun newInstance(): SplashScreenFragment = SplashScreenFragment()
    }
}
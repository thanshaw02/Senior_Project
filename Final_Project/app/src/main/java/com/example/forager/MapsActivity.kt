package com.example.forager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.forager.databinding.ActivityMapsBinding
import com.example.forager.misc.CameraAndViewport
import com.example.forager.misc.TypeAndStyles
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val typeAndStyles by lazy { TypeAndStyles() }
    private val  cameraAndViewport by lazy { CameraAndViewport() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // This method is called when there is a menu
    // We are overriding the function here
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
       menuInflater.inflate(R.menu.map_types_menu, menu)
        return true
    }

    // This is for the menu that changes the map type
    // This will not go in my release version!
    // Just here for practice purposes
    // Although I may use something like this for other features
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        typeAndStyles.setMapType(item, map)
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Finds the minimum and maximum zoom for the device the app is being viewed on
        val minZoom = map.minZoomLevel
        val maxZoom = map.maxZoomLevel

        // Add a marker in Sydney and move the camera
        val marquette = LatLng(46.5436, -87.3954)
        val newYork = LatLng(40.71614203933524, -74.0040676650565)
        map.addMarker(MarkerOptions().position(marquette).title("Marquette Marker"))
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(marquette, 10f))
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraAndViewport.marquette))

        map.uiSettings.apply {
            isZoomControlsEnabled = true
            // I think I want this enabled, need to come back to this
            // I'll first need to enable my-location layer
            // isMyLocationButtonEnabled = true
        }
        typeAndStyles.setMapStyle(map, this)

        // This is from the last video I watched in the Google API tutorial
        // Don't keep this!
        lifecycleScope.launch {
            delay(4000L)
            map.moveCamera(CameraUpdateFactory.newLatLng(newYork))
        }

        // I think I want some padding on the bottom for my buttons and other options
        // Keeping this commented so I can come back to it
        // map.setPadding(0, 0, 0, 300)
    }


}
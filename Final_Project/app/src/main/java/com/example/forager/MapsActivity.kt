package com.example.forager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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

    private val grav by lazy { Gravity() }

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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marquette, 10f))
        //map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraAndViewport.marquette))
        typeAndStyles.setMapStyle(map, this) // Sets the style of my map using raw JSON
        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }

        onMapClicked()
        onMapLongClicked()
    }

    private fun onMapClicked() {
        map.setOnMapClickListener {
            val latLong = it.latitude.toString() + ", " + it.longitude.toString()
            Toast.makeText(this, "Short click on: $latLong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onMapLongClicked() {
        map.setOnMapLongClickListener {
            map.addMarker(MarkerOptions().position(it).title("Your new marker!"))
            grav.apply {
                Toast.makeText(this@MapsActivity, "New marker at: ${it.longitude}, ${it.longitude}", Toast.LENGTH_SHORT).show()
            }

        }
    }
}

/*
map.uiSettings.apply {
    isZoomControlsEnabled = true
    // I think I want this enabled, need to come back to this
    // I'll first need to enable my-location layer
    // isMyLocationButtonEnabled = true
}
typeAndStyles.setMapStyle(map, this)

lifecycleScope.launch {
    delay(4000L)

    // animates the zooming in OR out depending on the zoom value given (between 0..20)
    //map.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)

    // This will animate the moving of the camera to a specified location in pixels, with a given duration and a GoogleMap.CancelableCallback
    //map.animateCamera(CameraUpdateFactory.scrollBy(200f, 0f), 2000, null)

    // This animates all of the properties from cameraAndViewport.marquette in CameraAndViewport.kt
    // In this final version I use a GoogleMaps.CancelableCallback as the callback, I override two functions "onFinish()" and "onCancel()" and display a Toast message
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraAndViewport.melbourne), 2000, object: GoogleMap.CancelableCallback {

        // Toast text will display when the camera animation is finished
        override fun onFinish() {
            Toast.makeText(this@MapsActivity, "Finished", Toast.LENGTH_SHORT).show()
        }

        // Toast text will display when/if the camera animation is canceled
        override fun onCancel() {
            Toast.makeText(this@MapsActivity, "Canceled", Toast.LENGTH_SHORT).show()
        }
    })

    //map.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraAndViewport.melbourneBounds, 100), 2000, null)
    // map.setLatLngBoundsForCameraTarget(cameraAndViewport.melbourneBounds)
*/


// This would all go in the "map.uiSettings.apply {  } block

// This is from the last video I watched in the Google API tutorial
// Don't keep this!
// lifecycleScope.launch {
//     delay(4000L)
//     map.moveCamera(CameraUpdateFactory.scrollBy(-1000f, 300f))
// }

// I think I want some padding on the bottom for my buttons and other options
// Keeping this commented so I can come back to it
// map.setPadding(0, 0, 0, 300)
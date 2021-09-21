package com.example.forager.misc

import android.content.Context
import android.util.Log
import android.view.MenuItem
import com.example.forager.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import java.lang.Exception

class TypeAndStyles {

    private val cameraAndViewport by lazy { CameraAndViewport() }

    fun setMapStyle(googleMap: GoogleMap, context: Context) {
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.style
                )
            )
            if(!success) Log.d("Maps", "Failed to add map style.")
        }
        catch (e: Exception) {
            Log.d("Maps", "Failed to add map style.")
        }
    }

    // This originally was doing the work up changing the map TYPES
    // Changed it to move to different locations using the "animateCamera()" function
    // Will want to use this when moving from pin to pin when the user pins a location!
    fun setMapType(item: MenuItem, googleMap: GoogleMap) {
        when(item.itemId) {
            R.id.jump_to_melbourne -> {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraAndViewport.melbourneBounds, 100), 8000, null)
            }
            R.id.jump_to_marquette -> {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraAndViewport.marquetteBounds, 100), 8000, null)
            }
//            R.id.normal_map -> {
//                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
//            }
//            R.id.hybrid_map -> {
//                googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
//            }
//            R.id.satellite_map -> {
//                googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
//            }
//            R.id.terrain_map -> {
//                googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
//            }
//            R.id.none_map -> {
//                googleMap.mapType = GoogleMap.MAP_TYPE_NONE
//            }
        }
    }

}
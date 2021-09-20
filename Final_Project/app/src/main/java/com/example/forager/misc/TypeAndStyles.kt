package com.example.forager.misc

import android.content.Context
import android.util.Log
import android.view.MenuItem
import com.example.forager.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import java.lang.Exception

class TypeAndStyles {

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

    fun setMapType(item: MenuItem, googleMap: GoogleMap) {
        when(item.itemId) {
            R.id.normal_map -> {
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            R.id.hybrid_map -> {
                googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
            R.id.satellite_map -> {
                googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
            R.id.terrain_map -> {
                googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
            R.id.none_map -> {
                googleMap.mapType = GoogleMap.MAP_TYPE_NONE
            }
        }
    }

}
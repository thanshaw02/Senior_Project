package com.example.forager.misc

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class CameraAndViewport {

    // Shows this location with 4 different properties
    val marquette: CameraPosition = CameraPosition.Builder()
        .target(LatLng(46.5436, -87.3954))
        .zoom(17f)
        .bearing(100f)
        .tilt(45f)
        .build()

    // This variable creates boundaries on the map, so this location will be shown within the two given LatLng objects
    val melbourneBounds = LatLngBounds(
        LatLng(-38.40613716330171, 144.35135749862158), // SW boundary
        LatLng(-37.54337046323397, 145.59418576748712) // NE boundary
    )

    val marquetteBounds = LatLngBounds(
        LatLng(46.50377756066286, -87.46555666105442), // SW boundary
        LatLng(46.60294828072716, -87.34861474255095) // NE boundary
    )

    val melbourne: CameraPosition = CameraPosition.Builder()
        .target(LatLng(-38.40613716330171, 144.35135749862158))
        .zoom(17f)
        .bearing(100f)
        .tilt(45f)
        .build()

}
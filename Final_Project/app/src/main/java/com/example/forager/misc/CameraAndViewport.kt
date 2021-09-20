package com.example.forager.misc

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class CameraAndViewport {

    val marquette: CameraPosition = CameraPosition.Builder()
        .target(LatLng(46.5436, -87.3954))
        .zoom(17f)
        .bearing(100f)
        .tilt(45f)
        .build()



}
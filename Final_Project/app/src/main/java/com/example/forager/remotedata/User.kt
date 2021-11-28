package com.example.forager.remotedata

/**
 * Use this link here to store and grab profile pictures from the user
 * https://stackoverflow.com/questions/60713365/loading-image-from-firebase-storage-not-loading-image-using-glide-using-kotlin
 */

data class User(
    var userName: String? = null,
    val fullName: String? = null,
    var email: String? = null,
    val dateCreated: String? = null,
    var numPlantsFound: String = "0"
)
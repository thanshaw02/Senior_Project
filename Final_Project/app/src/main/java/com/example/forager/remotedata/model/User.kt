package com.example.forager.remotedata.model

/**
 * Use this link here to store and grab profile pictures from the user
 * https://stackoverflow.com/questions/60713365/loading-image-from-firebase-storage-not-loading-image-using-glide-using-kotlin
 */

/**
 * Data class that holds all relevant information pertaining to a user. An instance of User is created
 * whenever someone first registers an account and retrieved whenever the user logs in again.
 *
 * @sample [com.example.forager.repository.register.RegisterActivity.writeNewUser]
 *
 * @param userName [String]?
 * @param fullName [String]?
 * @param email [String]?
 * @param dateCreated [String]?
 * @param numPlantsFound [Int]
 *
 * @author Tylor J. Hanshaw
 */
data class User(
    var userName: String? = null,
    val fullName: String? = null,
    var email: String? = null,
    val dateCreated: String? = null,
    var numPlantsFound: Int = 0
)


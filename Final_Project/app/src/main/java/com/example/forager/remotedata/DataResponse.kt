package com.example.forager.remotedata

import com.example.forager.remotedata.model.PlantListNode
import com.example.forager.remotedata.model.User
import com.google.firebase.database.DataSnapshot

/**
 * Data class used when retrieving data from my Realtime Database asynchronously.
 * One of the three parameters will be non-null, the call will either *return* a data snapshot,
 * [PlantListNode], or an exception.
 *
 * @param dataSnapshot [DataSnapshot]?
 * @param plants [MutableList<PlantListNode>]?
 * @param exception [Exception]?
 *
 * @author Tylor J. Hanshaw
 */
// This is used for obtaining the user's found plant list
data class DataResponse(
    var dataSnapshot: DataSnapshot? = null,
    var plants: MutableList<PlantListNode>? = null,
    var exception: Exception? = null
)

/**
 * Data class used when retrieving data from my Realtime Database asynchronously.
 * One of the two parameters will be non-null, the call will either *return* a [User],
 * or an exception.
 *
 * @param user [User]?
 * @param exception [Exception]?
 *
 * @author Tylor J. Hanshaw
 */
// This is used for obtaining the user's info
data class UserResponse(
    var user: User? = null,
    var exception: Exception? = null
)

/**
 * Data class used when retrieving data from my Realtime Database asynchronously.
 * One of the two parameters will be non-null, the call will either *return* a String,
 * or an exception.
 *
 * @param data [String]?
 * @param exception [Exception]?
 *
 * @author Tylor J. Hanshaw
 */
data class StringDataResponse(
    var data: String? = null,
    var exception: Exception? = null
)

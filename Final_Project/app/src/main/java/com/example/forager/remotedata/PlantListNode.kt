package com.example.forager.remotedata

import com.example.forager.localdata.model.Plant
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 *  The "isExpanded" variable is used for expanding the data in the RecyclerView
 */

@IgnoreExtraProperties
data class PlantListNode(
    val lat: Double,
    val long: Double,
    val plantAdded: Plant,
    val plantNotes: String,
    val dateFound: String? = null,
    @Exclude private var isExpanded: Boolean = false
) {

    @Exclude
    fun getIsExpanded(): Boolean = isExpanded

    @Exclude
    fun setIsExpanded(expand: Boolean) {
        isExpanded = expand
    }

}

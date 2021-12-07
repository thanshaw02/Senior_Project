package com.example.forager.localdata.model

import android.graphics.Bitmap
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data class that holds data pertaining to a plant, mainly used for displaying plants in
 * [PlantDatabaseFragment][com.example.forager.fragments.PlantDatabaseFragment]'s RecyclerView and
 * when adding a new plant to the user's [personal plant list.][com.example.forager.fragments.PersonalPlantListFragment].
 *
 * @see getIsExpanded
 * @see setIsExpanded
 *
 * @param commonName [String]
 * @param scientificName [String]
 * @param plantType [Int]?
 * @param plantColor [String]
 * @param sun [String]
 * @param height [String]
 * @param isExpanded Excluded variable used for displaying detail in the extended menu of plants in both
 * [PersonalPlantListFragment][com.example.forager.fragments.PersonalPlantListFragment] fragment and
 * [PlantDatabaseFragment][com.example.forager.fragments.PlantDatabaseFragment] fragment.
 *
 * @author Tylor J. Hanshaw
 */
@IgnoreExtraProperties
data class Plant(
    val commonName: String = "",
    val scientificName: String = "",
    val plantType: Int? = null,
    val plantColor: String = "",
    val sun: String = "",
    val height: String = "",
    @Exclude private val plantPhotoUri: String? = null
) {

    /**
     * Member function used to *get* the given plants photo URI.
     *
     * @see plantPhotoUri
     * @return [String]?
     */
    @Exclude
    fun getPlantPhotoUri(): String? = plantPhotoUri

}

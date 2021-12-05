package com.example.forager.remotedata.model

import com.example.forager.localdata.model.Plant
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

/**
 * Data class that holds data pertaining to plants the user has found and added to their personal
 * plant list. Similar to the [Plant] data class, but this data class holds the latitude and
 * longitude corresponding to where the user found the plant on the map. Nested within is an instance
 * of plant object, the date the plant was found on, and personal notes about the user's find if
 * they leave any.
 *
 * @see getUID
 * @see setUID
 * @see getIsExpanded
 * @see setIsExpanded
 *
 * @param lat [Double]
 * @param long [Double]
 * @param plantAdded [Plant]
 * @param plantNotes [String]
 * @param dateFound [String]
 * @param uid [UUID] - A unique identifier that is excluded from the RealtimeDatabase and instead used
 * as a unique 'key' when adding the plant to the user's personal plant list.
 * @param isExpanded Excluded variable used for displaying detail in the extended menu of plants in both
 * [PersonalPlantListFragment][com.example.forager.fragments.PersonalPlantListFragment] fragment and
 * [PlantDatabaseFragment][com.example.forager.fragments.PlantDatabaseFragment] fragment.
 *
 * @author Tylor J. Hanshaw
 */
@IgnoreExtraProperties
data class PlantListNode(
    val lat: Double = 0.0,
    val long: Double = 0.0,
    val plantAdded: Plant = Plant(),
    val plantNotes: String = "",
    val dateFound: String = "",
    val plantPhotoUri: String? = "", // If the user doesn't want to add a photo they do not have to
    @Exclude private var uid: String? = UUID.randomUUID().toString(),
    @Exclude private var isExpanded: Boolean = false
) {

    /**
     * Member function used to retrieve the [UUID][uid] of *this* PlantListNode, is excluded from
     * the RealtimeDatabase when adding a new plant.
     *
     * @sample [com.example.forager.repository.DataRepository.addPlantLocation]
     * @return [String]
     *
     * @author Tylor J. Hanshaw
     */
    @Exclude
    fun getUID(): String {
        return uid.toString()
    }

    /**
     * Member function used to set a given PlantListNode's [UUID][uid].
     *
     * @sample [com.example.forager.repository.DataRepository.getResponseFromDB]
     * @param newUid [String]?
     *
     * @author Tylor J. Hanshaw
     */
    @Exclude
    fun setUID(newUid: String?) {
        uid = newUid
    }

    /**
     * Member function used primarily to exclude the associated member variable [isExpanded]
     * from being added to my RealtimeDatabase when a new user finds a plant. But also used in
     * the [PlantDatabaseFragment][com.example.forager.fragments.PlantDatabaseFragment] to
     * open/close the extended menu for each plant.
     *
     * @sample [com.example.forager.fragments.PlantDatabaseFragment.PlantDBHolder.bindView]
     * @see isExpanded
     * @return [Boolean]
     *
     * @author Tylor J. Hanshaw
     */
    @Exclude
    fun getIsExpanded(): Boolean = isExpanded

    /**
     * Member function used primarily to exclude the associated member variable [isExpanded]
     * from being added to my RealtimeDatabase when a new user finds a plant. But also used in
     * the PlantDatabaseFragment to open/close the extended menu for each plant.
     *
     * @sample [com.example.forager.fragments.PlantDatabaseFragment.PlantDBHolder.bindView]
     * @param expand [Boolean]
     * @see isExpanded
     *
     * @author Tylor J. Hanshaw
     */
    @Exclude
    fun setIsExpanded(expand: Boolean) {
        isExpanded = expand
    }

}

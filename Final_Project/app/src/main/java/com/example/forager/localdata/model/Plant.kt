package com.example.forager.localdata.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Plant(
    val commonName: String = "",
    val scientificName: String = "",
    val plantType: Int,
    val plantColor: String = "",
    val sun: String = "",
    val height: String = "",
    @Exclude
    private var isExpanded: Boolean = false
){

    @Exclude
    fun getIsExpanded(): Boolean = isExpanded

    @Exclude
    fun setIsExpanded(expand: Boolean) {
        isExpanded = expand
    }

}

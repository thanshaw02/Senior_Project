/*
    TODO: Get my getSName() and getCName() working, also need to start up a UI for testing
    TODO: Get a repopulated dataset in here, but this is nto a huge deal right now
    TODO: Set up my fragments, get an idea of how I want to lay things out
    TODO: Finalize the schema I want to use for my database (although I think I'm almost where I want to be with it, may add location as a column)
*/

package com.example.forager.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "my_table")
data class Forager (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "scientific_name") val sName: String,
    @ColumnInfo(name = "common_name") val cName: String,
    @ColumnInfo(name = "date_found") val date: Date = Date(),
    @ColumnInfo(name = "num_found") val numFound: Int,
)
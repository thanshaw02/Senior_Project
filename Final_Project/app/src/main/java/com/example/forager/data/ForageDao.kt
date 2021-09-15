/*
    TODO: Get my getSName() and getCName() working, also need to start up a UI for testing
    TODO: Get a repopulated dataset in here, but this is nto a huge deal right now
    TODO: Set up my fragments, get an idea of how I want to lay things out
    TODO: Finalize the schema I want to use for my database (although I think I'm almost where I want to be with it, may add location as a column)
*/

package com.example.forager.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.forager.model.Forager

@Dao
interface ForageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlant(plant: Forager)

    @Update
    suspend fun updatePlant(plant: Forager)

    @Delete
    suspend fun deletePlant(plant: Forager)
/*
    // The idea here is to have the user search by scientific name
    @Query("SELECT * FROM my_table WHERE scientific_name LIKE :search")
    suspend fun grabSName(search: String?): LiveData<List<Forager>>

    // The idea here is to have the user search by common name
    @Query("SELECT * FROM my_table WHERE common_name LIKE :search")
    suspend fun grabCName(search: String?): LiveData<List<Forager>>
*/
    @Query("DELETE FROM my_table")
    suspend fun deleteAllPlants()

    @Query("SELECT * FROM my_table ORDER BY id ASC")
    fun readAllData(): LiveData<List<Forager>>

}
/*
    TODO: Get my getSName() and getCName() working, also need to start up a UI for testing
    TODO: Get a repopulated dataset in here, but this is nto a huge deal right now
    TODO: Set up my fragments, get an idea of how I want to lay things out
    TODO: Finalize the schema I want to use for my database (although I think I'm almost where I want to be with it, may add location as a column)
*/

package com.example.forager.repository

import  androidx.lifecycle.LiveData
import com.example.forager.data.ForageDao
import com.example.forager.model.Forager

class ForageRepository(private val forageDao: ForageDao) {

    val readAllData: LiveData<List<Forager>> = forageDao.readAllData()

    suspend fun addPlant(plant: Forager) = forageDao.addPlant(plant)

    suspend fun updatePlant(plant: Forager) = forageDao.updatePlant(plant)

    suspend fun deletePlant(plant: Forager) = forageDao.deletePlant(plant)
/*
    // Searching for a certain scientific name given user input
    suspend fun grabSName(search: String?) = forageDao.grabSName(search)

    // Searching for a certain common name given user input
    suspend fun grabCName(search: String?) = forageDao.grabCName(search)
*/
    suspend fun deleteAllPlants() = forageDao.deleteAllPlants()

}
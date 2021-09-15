/*
    TODO: Get my getSName() and getCName() working, also need to start up a UI for testing
    TODO: Get a repopulated dataset in here, but this is nto a huge deal right now
    TODO: Set up my fragments, get an idea of how I want to lay things out
    TODO: Finalize the schema I want to use for my database (although I think I'm almost where I want to be with it, may add location as a column)
*/

package com.example.forager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.forager.data.ForageDatabase
import com.example.forager.model.Forager
import com.example.forager.repository.ForageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForagerViewModel(application: Application): AndroidViewModel(application) {

    private val readAllData: LiveData<List<Forager>>
    private val repository: ForageRepository

    init {
        val foragerDao = ForageDatabase.getDatabase(application).forageDao()
        repository = ForageRepository(foragerDao)
        readAllData = repository.readAllData
    }

    fun addPlant(plant: Forager) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addPlant(plant)
        }
    }

    fun updatePlant(plant: Forager) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlant(plant)
        }
    }

    fun deletePlant(plant: Forager) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlant(plant)
        }
    }

    fun deleteAllPlants() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllPlants()
        }
    }
/*
    // I believe this must return what the search finds, will need to come back to this
    fun grabSName(search: String?)/*: LiveData<List<Forager>>*/ {
        viewModelScope.launch(Dispatchers.IO) {
            repository.grabSName(search)
        }
    }

    // I believe this must return what the search finds, will need to come back to this
    fun grabCName(search: String?)/*: LiveData<List<Forager>>*/ {
        viewModelScope.launch(Dispatchers.IO) {
            repository.grabCName(search)
        }
    }
*/
}
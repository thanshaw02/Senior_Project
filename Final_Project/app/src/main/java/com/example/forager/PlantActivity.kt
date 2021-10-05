package com.example.forager

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.example.forager.databinding.ActivityPlantBinding

class PlantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = ActivityPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

}
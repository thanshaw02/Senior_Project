/*
    TODO: Get my getSName() and getCName() working, also need to start up a UI for testing
    TODO: Get a repopulated dataset in here, but this is nto a huge deal right now
    TODO: Set up my fragments, get an idea of how I want to lay things out
    TODO: Finalize the schema I want to use for my database (although I think I'm almost where I want to be with it, may add location as a column)
*/

package com.example.forager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.forager.model.Forager
import com.example.forager.model.ForagerTypeConverter

@Database(entities = [Forager::class], version = 1, exportSchema = false)
@TypeConverters(ForagerTypeConverter::class)
abstract class ForageDatabase : RoomDatabase() {

    abstract fun forageDao(): ForageDao

    companion object {
        @Volatile
        private var INSTANCE: ForageDatabase? = null

        fun getDatabase(context: Context): ForageDatabase {
            val tempInstance = INSTANCE
            if(tempInstance != null) return tempInstance // If an instance of the database has been created, then we return that instance
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ForageDatabase::class.java,
                    "my_table",
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}
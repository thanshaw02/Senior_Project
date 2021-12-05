package com.example.forager.localdata

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import android.content.SharedPreferences
import android.util.Log

private const val LOG = "PlantDatabaseHelper:"

/**
 * A helper class to manage database creation and version management.
 *
 * @param context [Context]
 *
 * @author Tylor J. Hanshaw
 */
class PlantsDatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    /**
     * Companion object used to store the SQLite database name and current version.
     *
     * @see DB_VERSION
     * @see DB_NAME
     */
    companion object {
        private const val DB_VERSION = 1
        const val DB_NAME = "Internal_Plant_DB_PHOTOS.db"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "${context.packageName}.database_version",
        Context.MODE_PRIVATE
    )

    // Checks to see if the current version is out of date or not
    private fun installedDatabaseIsOutdated(): Boolean {
        return preferences.getInt(DB_NAME, 0) < DB_VERSION
    }

    // Writes the current database version
    private fun writeDatabaseVersionInPreferences() {
        preferences.edit().apply {
            putInt(DB_NAME, DB_VERSION)
            apply()
        }
    }

    @Synchronized
    private fun installOrUpdateIfNecessary() {
        if(installedDatabaseIsOutdated()) {
            context.deleteDatabase(DB_NAME)
            installDatabaseFromAssets()
            writeDatabaseVersionInPreferences()
        }
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        throw RuntimeException("The $DB_NAME database is not writable.")
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        installOrUpdateIfNecessary()
        return super.getReadableDatabase()
    }

    private fun installDatabaseFromAssets() {
        val inputStream = context.assets.open("database/$DB_NAME")
        try {
            val outputFile = File(context.getDatabasePath(DB_NAME).path)
            val outputStream = FileOutputStream(outputFile)

            inputStream.copyTo(outputStream)
            inputStream.close()

            outputStream.flush()
            outputStream.close()

            Log.d(LOG, "Installing the database..")
        } catch (exception: Throwable) {
            throw RuntimeException("The $DB_NAME database couldn't be installed.", exception)
        }
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        Log.d(LOG, "Nothing to do here, we are not creating a database..")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        Log.d(LOG, "Nothing to do here, we are not updating the database..")
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
    }

}
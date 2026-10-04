package com.smartsolar.stations.stations.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.stations.stations.model.SolarStation

/**
 * Local SQLite cache only. Central data lives in MongoDB Atlas (SmartSolarDB)
 * via the ASP.NET Core API. Tables: LocalUser session mirror + station cache.
 * Booking storage belongs to Member 3 (reservations).
 */
class StationsDbHelper(context: Context) :
    SQLiteOpenHelper(context, "smartsolar.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE LocalUser(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nic TEXT UNIQUE, name TEXT, email TEXT, role TEXT, token TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE StationCache(
                stationId TEXT PRIMARY KEY, name TEXT, location TEXT,
                total REAL, available REAL, status TEXT, updatedAt INTEGER)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS LocalUser")
        db.execSQL("DROP TABLE IF EXISTS StationCache")
        db.execSQL("DROP TABLE IF EXISTS LocalBooking")
        onCreate(db)
    }

    fun cacheStations(stations: List<SolarStation>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            stations.forEach {
                val v = ContentValues().apply {
                    put("stationId", it.stationId)
                    put("name", it.stationName)
                    put("location", it.location)
                    put("total", it.totalCapacity)
                    put("available", it.availableCapacity)
                    put("status", it.status)
                    put("updatedAt", System.currentTimeMillis())
                }
                db.insertWithOnConflict("StationCache", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun readCachedStations(): List<SolarStation> {
        val out = mutableListOf<SolarStation>()
        readableDatabase.rawQuery("SELECT * FROM StationCache ORDER BY stationId", null).use { c ->
            while (c.moveToNext()) {
                out += SolarStation(
                    stationId = c.getString(0),
                    stationName = c.getString(1),
                    location = c.getString(2),
                    totalCapacity = c.getDouble(3),
                    availableCapacity = c.getDouble(4),
                    status = c.getString(5)
                )
            }
        }
        return out
    }
}

package com.smartsolar.stations.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.models.SolarStation

/**
 * Local SQLite cache only. Central data lives in MongoDB Atlas (SmartSolarDB)
 * via the ASP.NET Core API. Tables: LocalUser session mirror + station cache
 * + booking cache (Member 3 owns booking writes).
 */
class StationsDbHelper(context: Context) :
    SQLiteOpenHelper(context, "smartsolar.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE LocalUser(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nic TEXT UNIQUE, name TEXT, email TEXT, role TEXT, token TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE StationCache(
                stationId TEXT PRIMARY KEY, name TEXT, location TEXT,
                latitude REAL, longitude REAL,
                total REAL, available REAL, status TEXT, updatedAt INTEGER)"""
        )
        db.execSQL(
            """CREATE TABLE LocalBooking(
                reservationId TEXT PRIMARY KEY, stationId TEXT,
                date TEXT, time TEXT, status TEXT)"""
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
                    put("latitude", it.latitude)
                    put("longitude", it.longitude)
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
                    latitude = c.getDouble(3),
                    longitude = c.getDouble(4),
                    totalCapacity = c.getDouble(5),
                    availableCapacity = c.getDouble(6),
                    status = c.getString(7)
                )
            }
        }
        return out
    }

    fun cacheBookings(bookings: List<Booking>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            bookings.forEach {
                val v = ContentValues().apply {
                    put("reservationId", it.reservationId)
                    put("stationId", it.stationId)
                    put("date", it.date)
                    put("time", it.time)
                    put("status", it.status)
                }
                db.insertWithOnConflict("LocalBooking", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

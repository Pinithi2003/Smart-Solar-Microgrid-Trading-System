package com.smartsolar.stations.reservations.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.stations.reservations.model.EnergyReservation

/** Local copy of bookings. The API remains the source of truth. */
class ReservationDbHelper(context: Context) : SQLiteOpenHelper(context, "reservations.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE ReservationCache(
                reservationId TEXT PRIMARY KEY,
                userId TEXT, stationId TEXT, slotId TEXT, date TEXT,
                startTime TEXT, endTime TEXT, energyAmount REAL,
                status TEXT, verificationCode TEXT)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ReservationCache")
        onCreate(db)
    }

    fun replaceAll(items: List<EnergyReservation>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("ReservationCache", null, null)
            items.forEach { item ->
                val values = ContentValues().apply {
                    put("reservationId", item.reservationId)
                    put("userId", item.userId)
                    put("stationId", item.stationId)
                    put("slotId", item.slotId)
                    put("date", item.date)
                    put("startTime", item.startTime)
                    put("endTime", item.endTime)
                    put("energyAmount", item.energyAmount)
                    put("status", item.status)
                    put("verificationCode", item.verificationCode)
                }
                db.insert("ReservationCache", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun readAll(): List<EnergyReservation> {
        val out = mutableListOf<EnergyReservation>()
        readableDatabase.rawQuery("SELECT * FROM ReservationCache ORDER BY date", null).use { cursor ->
            while (cursor.moveToNext()) {
                out += EnergyReservation(
                    reservationId = cursor.getString(0),
                    userId = cursor.getString(1).orEmpty(),
                    stationId = cursor.getString(2).orEmpty(),
                    slotId = cursor.getString(3).orEmpty(),
                    date = cursor.getString(4).orEmpty(),
                    startTime = cursor.getString(5).orEmpty(),
                    endTime = cursor.getString(6).orEmpty(),
                    energyAmount = cursor.getDouble(7),
                    status = cursor.getString(8).orEmpty(),
                    verificationCode = cursor.getString(9).orEmpty()
                )
            }
        }
        return out
    }
}

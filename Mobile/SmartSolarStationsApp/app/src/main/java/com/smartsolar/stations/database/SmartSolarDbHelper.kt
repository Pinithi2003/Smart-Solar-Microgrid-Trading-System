package com.smartsolar.stations.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.stations.models.Booking
import com.smartsolar.stations.models.LocalUser
import com.smartsolar.stations.models.SolarStation

/**
 * SQLite local database helper for the Smart Solar Microgrid Trading System.
 * Manages LocalUser, StationCache, and LocalBooking tables.
 * Preloads realistic demo data for offline execution.
 */
class SmartSolarDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "smartsolar_enterprise.db"
        const val DATABASE_VERSION = 2

        // Tables
        const val TABLE_USERS = "LocalUser"
        const val TABLE_STATIONS = "StationCache"
        const val TABLE_BOOKINGS = "LocalBooking"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Users Table
        db.execSQL(
            """CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nic TEXT UNIQUE,
                name TEXT,
                email TEXT UNIQUE,
                phone TEXT,
                role TEXT,
                password TEXT,
                isActive INTEGER DEFAULT 1,
                token TEXT
            )"""
        )

        // 2. Stations Cache Table
        db.execSQL(
            """CREATE TABLE $TABLE_STATIONS (
                stationId TEXT PRIMARY KEY,
                name TEXT,
                location TEXT,
                latitude REAL,
                longitude REAL,
                total REAL,
                available REAL,
                status TEXT,
                operator TEXT,
                distanceKm TEXT
            )"""
        )

        // 3. Bookings Table
        db.execSQL(
            """CREATE TABLE $TABLE_BOOKINGS (
                reservationId TEXT PRIMARY KEY,
                stationId TEXT,
                stationName TEXT,
                prosumerNic TEXT,
                prosumerName TEXT,
                date TEXT,
                time TEXT,
                energyAmount REAL,
                status TEXT,
                transactionId TEXT,
                createdDate TEXT,
                verifiedAt TEXT,
                completedAt TEXT,
                operatorId TEXT,
                notes TEXT
            )"""
        )

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Seed Users
        insertUserRaw(db, LocalUser(1, "200012345678", "Nimal Perera", "nimal@example.com", "+94 77 123 4567", "Prosumer", "123456", true))
        insertUserRaw(db, LocalUser(2, "199512345678", "Kamal Silva", "operator@example.com", "+94 71 987 6543", "GridOperator", "123456", true))

        // Seed Stations
        val stations = listOf(
            SolarStation("ST001", "Grid Node A (Colombo)", "Colombo Microgrid Station", 6.9271, 79.8612, 100.0, 65.0, "Active", "Grid Operator"),
            SolarStation("ST002", "Grid Node B (Kalutara)", "Kalutara Feeder Hub", 6.5854, 79.9607, 120.0, 48.0, "Active", "Grid Operator"),
            SolarStation("ST003", "Grid Node C (Galle)", "Galle Coastal Microgrid", 6.0535, 80.2210, 90.0, 32.0, "Maintenance", "Grid Operator"),
            SolarStation("ST004", "Grid Node D (Kandy)", "Central Highlands Solar Sub", 7.2906, 80.6337, 110.0, 74.0, "Active", "Grid Operator")
        )
        val distances = mapOf("ST001" to "1.2 km", "ST002" to "2.8 km", "ST003" to "4.1 km", "ST004" to "8.5 km")

        for (st in stations) {
            val v = ContentValues().apply {
                put("stationId", st.stationId)
                put("name", st.stationName)
                put("location", st.location)
                put("latitude", st.latitude)
                put("longitude", st.longitude)
                put("total", st.totalCapacity)
                put("available", st.availableCapacity)
                put("status", st.status)
                put("operator", st.operator)
                put("distanceKm", distances[st.stationId] ?: "3.0 km")
            }
            db.insert(TABLE_STATIONS, null, v)
        }

        // Seed Demo Reservations
        val bookings = listOf(
            Booking("RES10001", "ST001", "Grid Node A", "200012345678", "Nimal Perera", "25 September 2026", "10:00 AM – 11:00 AM", 15.5, "APPROVED", "TXN-2026-00001", "2026-09-20"),
            Booking("RES10002", "ST002", "Grid Node B", "200012345678", "Nimal Perera", "26 September 2026", "02:00 PM – 03:00 PM", 20.0, "PENDING", null, "2026-09-21"),
            Booking("RES10003", "ST001", "Grid Node A", "200012345678", "Nimal Perera", "20 September 2026", "11:00 AM – 12:00 PM", 10.0, "COMPLETED", "TXN-2026-00000", "2026-09-18", "2026-09-20 11:15 AM", "2026-09-20 11:55 AM", "OPR-FIELD-01", "Transfer completed normally."),
            Booking("RES10004", "ST002", "Grid Node B", "200012345678", "Nimal Perera", "18 September 2026", "09:00 AM – 10:00 AM", 25.0, "COMPLETED", "TXN-2026-00004", "2026-09-15", "2026-09-18 09:10 AM", "2026-09-18 09:50 AM", "OPR-FIELD-01", "All cells discharged cleanly."),
            Booking("RES10005", "ST001", "Grid Node A", "200012345678", "Nimal Perera", "15 September 2026", "03:00 PM – 04:00 PM", 12.0, "CANCELLED", "TXN-2026-00005", "2026-09-12"),
            Booking("RES10006", "ST003", "Grid Node C", "200012345678", "Nimal Perera", "19 September 2026", "01:00 PM – 02:00 PM", 18.0, "COMPLETED", "TXN-2026-00006", "2026-09-16", "2026-09-19 01:05 PM", "2026-09-19 01:50 PM", "OPR-FIELD-01", "High efficiency injection."),
            Booking("RES10007", "ST004", "Grid Node D", "200012345678", "Nimal Perera", "21 September 2026", "10:00 AM – 11:00 AM", 30.0, "COMPLETED", "TXN-2026-00007", "2026-09-19", "2026-09-21 10:10 AM", "2026-09-21 10:55 AM", "OPR-FIELD-01", "Dispatched to regional microgrid."),
            Booking("RES10008", "ST001", "Grid Node A", "200012345678", "Nimal Perera", "21 September 2026", "02:00 PM – 03:00 PM", 14.5, "COMPLETED", "TXN-2026-00008", "2026-09-20", "2026-09-21 02:15 PM", "2026-09-21 02:50 PM", "OPR-FIELD-01", "Completed.")
        )

        for (b in bookings) {
            insertBookingRaw(db, b)
        }
    }

    private fun insertUserRaw(db: SQLiteDatabase, u: LocalUser) {
        val v = ContentValues().apply {
            put("nic", u.nic)
            put("name", u.name)
            put("email", u.email)
            put("phone", u.phone)
            put("role", u.role)
            put("password", u.password)
            put("isActive", if (u.isActive) 1 else 0)
            put("token", u.token)
        }
        db.insertWithOnConflict(TABLE_USERS, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun insertBookingRaw(db: SQLiteDatabase, b: Booking) {
        val v = ContentValues().apply {
            put("reservationId", b.reservationId)
            put("stationId", b.stationId)
            put("stationName", b.stationName)
            put("prosumerNic", b.prosumerNic)
            put("prosumerName", b.prosumerName)
            put("date", b.date)
            put("time", b.time)
            put("energyAmount", b.energyAmount)
            put("status", b.status)
            put("transactionId", b.transactionId)
            put("createdDate", b.createdDate)
            put("verifiedAt", b.verifiedAt)
            put("completedAt", b.completedAt)
            put("operatorId", b.operatorId)
            put("notes", b.notes)
        }
        db.insertWithOnConflict(TABLE_BOOKINGS, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ==================== USER OPERATIONS ====================

    fun getUserByNic(nic: String): LocalUser? {
        readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE nic = ? LIMIT 1", arrayOf(nic)).use { c ->
            if (c.moveToFirst()) return cursorToUser(c)
        }
        return null
    }

    fun getUserByEmail(email: String): LocalUser? {
        readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE LOWER(email) = LOWER(?) LIMIT 1", arrayOf(email)).use { c ->
            if (c.moveToFirst()) return cursorToUser(c)
        }
        return null
    }

    fun insertUser(user: LocalUser): Boolean {
        val v = ContentValues().apply {
            put("nic", user.nic)
            put("name", user.name)
            put("email", user.email)
            put("phone", user.phone)
            put("role", user.role)
            put("password", user.password)
            put("isActive", if (user.isActive) 1 else 0)
            put("token", user.token)
        }
        return writableDatabase.insert(TABLE_USERS, null, v) != -1L
    }

    fun updateUserProfile(nic: String, name: String, email: String, phone: String): Boolean {
        val v = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("phone", phone)
        }
        return writableDatabase.update(TABLE_USERS, v, "nic = ?", arrayOf(nic)) > 0
    }

    fun deactivateUser(nic: String): Boolean {
        val v = ContentValues().apply { put("isActive", 0) }
        return writableDatabase.update(TABLE_USERS, v, "nic = ?", arrayOf(nic)) > 0
    }

    private fun cursorToUser(c: Cursor): LocalUser {
        return LocalUser(
            id = c.getLong(0),
            nic = c.getString(1),
            name = c.getString(2),
            email = c.getString(3),
            phone = c.getString(4),
            role = c.getString(5),
            password = c.getString(6),
            isActive = c.getInt(7) == 1,
            token = c.getString(8)
        )
    }

    // ==================== STATIONS OPERATIONS ====================

    fun getAllStations(): List<SolarStation> {
        val list = mutableListOf<SolarStation>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_STATIONS ORDER BY stationId ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SolarStation(
                        stationId = c.getString(0),
                        stationName = c.getString(1),
                        location = c.getString(2),
                        latitude = c.getDouble(3),
                        longitude = c.getDouble(4),
                        totalCapacity = c.getDouble(5),
                        availableCapacity = c.getDouble(6),
                        status = c.getString(7),
                        operator = c.getString(8)
                    )
                )
            }
        }
        return list
    }

    fun getStationById(id: String): SolarStation? {
        readableDatabase.rawQuery("SELECT * FROM $TABLE_STATIONS WHERE stationId = ? LIMIT 1", arrayOf(id)).use { c ->
            if (c.moveToFirst()) {
                return SolarStation(
                    stationId = c.getString(0),
                    stationName = c.getString(1),
                    location = c.getString(2),
                    latitude = c.getDouble(3),
                    longitude = c.getDouble(4),
                    totalCapacity = c.getDouble(5),
                    availableCapacity = c.getDouble(6),
                    status = c.getString(7),
                    operator = c.getString(8)
                )
            }
        }
        return null
    }

    // ==================== BOOKING OPERATIONS ====================

    fun insertBooking(b: Booking): Boolean {
        val v = ContentValues().apply {
            put("reservationId", b.reservationId)
            put("stationId", b.stationId)
            put("stationName", b.stationName)
            put("prosumerNic", b.prosumerNic)
            put("prosumerName", b.prosumerName)
            put("date", b.date)
            put("time", b.time)
            put("energyAmount", b.energyAmount)
            put("status", b.status)
            put("transactionId", b.transactionId)
            put("createdDate", b.createdDate)
            put("verifiedAt", b.verifiedAt)
            put("completedAt", b.completedAt)
            put("operatorId", b.operatorId)
            put("notes", b.notes)
        }
        return writableDatabase.insert(TABLE_BOOKINGS, null, v) != -1L
    }

    fun getBookingsByProsumer(nic: String, statusFilter: String? = null): List<Booking> {
        val list = mutableListOf<Booking>()
        val sql = if (statusFilter.isNullOrBlank() || statusFilter.equals("ALL", ignoreCase = true)) {
            "SELECT * FROM $TABLE_BOOKINGS WHERE prosumerNic = ? ORDER BY reservationId DESC"
        } else {
            "SELECT * FROM $TABLE_BOOKINGS WHERE prosumerNic = ? AND UPPER(status) = UPPER('$statusFilter') ORDER BY reservationId DESC"
        }

        readableDatabase.rawQuery(sql, arrayOf(nic)).use { c ->
            while (c.moveToNext()) {
                list.add(cursorToBooking(c))
            }
        }
        return list
    }

    fun getAllBookings(): List<Booking> {
        val list = mutableListOf<Booking>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_BOOKINGS ORDER BY reservationId DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(cursorToBooking(c))
            }
        }
        return list
    }

    fun getBookingById(resId: String): Booking? {
        readableDatabase.rawQuery("SELECT * FROM $TABLE_BOOKINGS WHERE reservationId = ? LIMIT 1", arrayOf(resId)).use { c ->
            if (c.moveToFirst()) return cursorToBooking(c)
        }
        return null
    }

    fun getBookingByTransactionId(txnId: String): Booking? {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_BOOKINGS WHERE transactionId = ? OR reservationId = ? LIMIT 1",
            arrayOf(txnId, txnId)
        ).use { c ->
            if (c.moveToFirst()) return cursorToBooking(c)
        }
        return null
    }

    fun updateBookingStatus(resId: String, newStatus: String): Boolean {
        val v = ContentValues().apply { put("status", newStatus) }
        return writableDatabase.update(TABLE_BOOKINGS, v, "reservationId = ?", arrayOf(resId)) > 0
    }

    fun updateBookingDetails(resId: String, newDate: String, newTime: String, energyAmount: Double): Boolean {
        val v = ContentValues().apply {
            put("date", newDate)
            put("time", newTime)
            put("energyAmount", energyAmount)
        }
        return writableDatabase.update(TABLE_BOOKINGS, v, "reservationId = ?", arrayOf(resId)) > 0
    }

    fun completeEnergyTransfer(resId: String, operatorId: String, notes: String?): Boolean {
        val v = ContentValues().apply {
            put("status", "COMPLETED")
            put("completedAt", System.currentTimeMillis().toString())
            put("operatorId", operatorId)
            put("notes", notes)
        }
        return writableDatabase.update(TABLE_BOOKINGS, v, "reservationId = ?", arrayOf(resId)) > 0
    }

    fun countActiveBookings(nic: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BOOKINGS WHERE prosumerNic = ? AND status IN ('PENDING', 'APPROVED')",
            arrayOf(nic)
        ).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    private fun cursorToBooking(c: Cursor): Booking {
        return Booking(
            reservationId = c.getString(0),
            stationId = c.getString(1),
            stationName = c.getString(2) ?: "",
            prosumerNic = c.getString(3) ?: "",
            prosumerName = c.getString(4) ?: "",
            date = c.getString(5) ?: "",
            time = c.getString(6) ?: "",
            energyAmount = c.getDouble(7),
            status = c.getString(8) ?: "PENDING",
            transactionId = c.getString(9),
            createdDate = c.getString(10) ?: "",
            verifiedAt = c.getString(11),
            completedAt = c.getString(12),
            operatorId = c.getString(13),
            notes = c.getString(14)
        )
    }
}

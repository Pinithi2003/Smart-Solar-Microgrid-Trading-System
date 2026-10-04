package com.smartsolar.stations.auth.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.stations.auth.model.LocalUser

class LocalDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NIC TEXT,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PHONE TEXT,
                $COLUMN_ROLE TEXT NOT NULL,
                $COLUMN_STATUS TEXT,
                $COLUMN_IS_ACTIVE INTEGER NOT NULL,
                $COLUMN_TOKEN TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createUserTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL(
                "ALTER TABLE $TABLE_USER ADD COLUMN $COLUMN_PHONE TEXT DEFAULT ''"
            )
        }
    }

    fun saveUser(user: LocalUser) {

        val db = writableDatabase

        db.delete(
            TABLE_USER,
            null,
            null
        )

        val values = ContentValues().apply {

            put(COLUMN_ID, user.id)
            put(COLUMN_NIC, user.nic)
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_ROLE, user.role)
            put(COLUMN_STATUS, user.status)
            put(
                COLUMN_IS_ACTIVE,
                if (user.isActive) 1 else 0
            )
            put(COLUMN_TOKEN, user.token)
        }

        db.insert(
            TABLE_USER,
            null,
            values
        )

        db.close()
    }

    fun getUser(): LocalUser? {

        val db = readableDatabase

        val cursor = db.query(
            TABLE_USER,
            null,
            null,
            null,
            null,
            null,
            null
        )

        var user: LocalUser? = null

        if (cursor.moveToFirst()) {

            user = LocalUser(

                id = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_ID)
                ),

                nic = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_NIC)
                ),

                fullName = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)
                ),

                email = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_EMAIL)
                ),

                phone = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_PHONE)
                ),

                role = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_ROLE)
                ),

                status = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_STATUS)
                ),

                isActive = cursor.getInt(
                    cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)
                ) == 1,

                token = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_TOKEN)
                )
            )
        }

        cursor.close()
        db.close()

        return user
    }

    fun clearUser() {

        val db = writableDatabase

        db.delete(
            TABLE_USER,
            null,
            null
        )

        db.close()
    }

    companion object {

        private const val DATABASE_NAME = "SmartSolarLocal.db"

        private const val DATABASE_VERSION = 2

        private const val TABLE_USER = "local_user"

        private const val COLUMN_ID = "id"

        private const val COLUMN_NIC = "nic"

        private const val COLUMN_FULL_NAME = "full_name"

        private const val COLUMN_EMAIL = "email"

        private const val COLUMN_PHONE = "phone"

        private const val COLUMN_ROLE = "role"

        private const val COLUMN_STATUS = "status"

        private const val COLUMN_IS_ACTIVE = "is_active"

        private const val COLUMN_TOKEN = "token"
    }
}
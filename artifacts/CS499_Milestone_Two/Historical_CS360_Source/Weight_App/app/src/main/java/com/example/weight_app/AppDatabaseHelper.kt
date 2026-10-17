package com.example.weight_app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "weight_tracker.db"
        private const val DB_VERSION = 1

        // ---- Users table ----
        private const val T_USERS = "users"
        private const val C_USER_ID = "id"
        private const val C_USERNAME = "username"
        private const val C_PASSWORD = "password"

        // ---- Weights table ----
        private const val T_WEIGHTS = "weights"
        const val C_WEIGHT_ID = "id"
        const val C_WEIGHT_USER_ID = "user_id"
        const val C_DATE = "date"
        const val C_WEIGHT = "weight"

        // ---- Goals table ----
        private const val T_GOALS = "goals"
        private const val C_GOAL_ID = "id"
        private const val C_GOAL_USER_ID = "user_id"
        private const val C_GOAL_WEIGHT = "goal_weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_USERS (
                $C_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_USERNAME TEXT UNIQUE NOT NULL,
                $C_PASSWORD TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $T_WEIGHTS (
                $C_WEIGHT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_WEIGHT_USER_ID INTEGER NOT NULL,
                $C_DATE TEXT NOT NULL,
                $C_WEIGHT REAL NOT NULL,
                FOREIGN KEY($C_WEIGHT_USER_ID) REFERENCES $T_USERS($C_USER_ID)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $T_GOALS (
                $C_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_GOAL_USER_ID INTEGER UNIQUE NOT NULL,
                $C_GOAL_WEIGHT REAL NOT NULL,
                FOREIGN KEY($C_GOAL_USER_ID) REFERENCES $T_USERS($C_USER_ID)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple reset for coursework
        db.execSQL("DROP TABLE IF EXISTS $T_GOALS")
        db.execSQL("DROP TABLE IF EXISTS $T_WEIGHTS")
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        onCreate(db)
    }

    // ---------------- LOGIN / ACCOUNT CREATION ----------------

    fun createUserIfNotExists(username: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(C_USERNAME, username)
            put(C_PASSWORD, password)
        }

        return try {
            db.insertOrThrow(T_USERS, null, values) != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun validateLogin(username: String, password: String): Int? {
        val db = readableDatabase
        val cursor = db.query(
            T_USERS,
            arrayOf(C_USER_ID),
            "$C_USERNAME=? AND $C_PASSWORD=?",
            arrayOf(username, password),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else null
        }
    }

    // ---------------- WEIGHTS CRUD ----------------

    fun insertWeight(userId: Int, date: String, weight: Double): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(C_WEIGHT_USER_ID, userId)
            put(C_DATE, date)
            put(C_WEIGHT, weight)
        }
        return db.insert(T_WEIGHTS, null, values)
    }

    fun readAllWeights(userId: Int): Cursor {
        val db = readableDatabase
        return db.query(
            T_WEIGHTS,
            arrayOf(C_WEIGHT_ID, C_DATE, C_WEIGHT),
            "$C_WEIGHT_USER_ID=?",
            arrayOf(userId.toString()),
            null,
            null,
            "$C_DATE DESC"
        )
    }

    fun updateWeight(rowId: Int, date: String, weight: Double): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(C_DATE, date)
            put(C_WEIGHT, weight)
        }
        return db.update(T_WEIGHTS, values, "$C_WEIGHT_ID=?", arrayOf(rowId.toString()))
    }

    fun deleteWeight(rowId: Int): Int {
        val db = writableDatabase
        return db.delete(T_WEIGHTS, "$C_WEIGHT_ID=?", arrayOf(rowId.toString()))
    }

    // ---------------- GOAL ----------------

    fun upsertGoal(userId: Int, goalWeight: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(C_GOAL_USER_ID, userId)
            put(C_GOAL_WEIGHT, goalWeight)
        }

        val updated = db.update(T_GOALS, values, "$C_GOAL_USER_ID=?", arrayOf(userId.toString()))
        if (updated == 0) {
            db.insert(T_GOALS, null, values)
        }
    }

    fun getGoal(userId: Int): Double? {
        val db = readableDatabase
        val cursor = db.query(
            T_GOALS,
            arrayOf(C_GOAL_WEIGHT),
            "$C_GOAL_USER_ID=?",
            arrayOf(userId.toString()),
            null, null, null
        )

        cursor.use {
            return if (it.moveToFirst()) it.getDouble(0) else null
        }
    }
}

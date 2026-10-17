package com.example.weight_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeParseException

class DataGridActivity : ComponentActivity() {

    private lateinit var db: AppDatabaseHelper
    private lateinit var adapter: WeightAdapter
    private lateinit var dateInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var goalInput: EditText
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_grid)

        userId = intent.getIntExtra(USER_ID_EXTRA, -1)
        if (userId == -1) {
            Toast.makeText(this, "Unable to identify the logged-in user.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db = AppDatabaseHelper(this)
        dateInput = findViewById(R.id.editTextNewDate)
        weightInput = findViewById(R.id.editTextNewWeight)
        goalInput = findViewById(R.id.editTextGoalWeight)

        adapter = WeightAdapter(
            items = mutableListOf(),
            onDelete = { entry -> deleteEntry(entry) },
            onUpdate = { entry, date, weight -> updateEntry(entry, date, weight) }
        )

        findViewById<RecyclerView>(R.id.recyclerViewGrid).apply {
            layoutManager = LinearLayoutManager(this@DataGridActivity)
            adapter = this@DataGridActivity.adapter
        }

        findViewById<Button>(R.id.buttonAddEntry).setOnClickListener { addEntry() }
        findViewById<Button>(R.id.buttonSaveGoal).setOnClickListener { saveGoal() }
        findViewById<Button>(R.id.buttonSmsSettings).setOnClickListener {
            startActivity(Intent(this, SmsPermissionActivity::class.java))
        }

        showSavedGoal()
        loadWeights()
    }

    private fun addEntry() {
        val date = dateInput.text.toString().trim()
        val weight = weightInput.text.toString().trim().toDoubleOrNull()

        if (!isValidDate(date)) {
            dateInput.error = "Use a valid date in YYYY-MM-DD format."
            return
        }
        if (!isValidWeight(weight)) {
            weightInput.error = "Enter a weight between 1 and 2000."
            return
        }
        val validWeight = weight!!

        val rowId = db.insertWeight(userId, date, validWeight)
        if (rowId == -1L) {
            Toast.makeText(this, "The weight entry could not be saved.", Toast.LENGTH_SHORT).show()
            return
        }

        dateInput.text.clear()
        weightInput.text.clear()
        loadWeights()
        notifyIfGoalReached(validWeight)
        Toast.makeText(this, "Weight entry added.", Toast.LENGTH_SHORT).show()
    }

    private fun saveGoal() {
        val goal = goalInput.text.toString().trim().toDoubleOrNull()
        if (!isValidWeight(goal)) {
            goalInput.error = "Enter a goal between 1 and 2000."
            return
        }

        if (db.upsertGoal(userId, goal!!)) {
            Toast.makeText(this, "Goal saved.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "The goal could not be saved.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEntry(entry: WeightEntry, date: String, weight: Double) {
        if (!isValidDate(date) || !isValidWeight(weight)) {
            Toast.makeText(this, "Enter a valid date and weight.", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = db.updateWeight(userId, entry.id, date, weight)
        if (updated > 0) {
            loadWeights()
            notifyIfGoalReached(weight)
            Toast.makeText(this, "Weight entry updated.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "The weight entry could not be updated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteEntry(entry: WeightEntry) {
        val deleted = db.deleteWeight(userId, entry.id)
        if (deleted > 0) {
            loadWeights()
            Toast.makeText(this, "Weight entry deleted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "The weight entry could not be deleted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadWeights() {
        val entries = mutableListOf<WeightEntry>()
        db.readAllWeights(userId).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.C_WEIGHT_ID)
            val dateIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.C_DATE)
            val weightIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.C_WEIGHT)

            while (cursor.moveToNext()) {
                entries.add(
                    WeightEntry(
                        id = cursor.getInt(idIndex),
                        date = cursor.getString(dateIndex),
                        weight = cursor.getDouble(weightIndex)
                    )
                )
            }
        }
        adapter.setItems(entries)
    }

    private fun showSavedGoal() {
        db.getGoal(userId)?.let { goalInput.setText(formatWeight(it)) }
    }

    private fun notifyIfGoalReached(weight: Double) {
        val goal = db.getGoal(userId) ?: return
        if (weight <= goal) {
            SmsPermissionActivity.sendGoalReachedSmsIfAllowed(
                this,
                "Congratulations! You reached your goal weight of ${formatWeight(goal)}."
            )
        }
    }

    private fun isValidDate(value: String): Boolean {
        return try {
            LocalDate.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun isValidWeight(value: Double?): Boolean {
        return value?.let { it.isFinite() && it in MIN_WEIGHT..MAX_WEIGHT } ?: false
    }

    private fun formatWeight(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }

    companion object {
        private const val USER_ID_EXTRA = "USER_ID"
        private const val MIN_WEIGHT = 1.0
        private const val MAX_WEIGHT = 2000.0
    }
}

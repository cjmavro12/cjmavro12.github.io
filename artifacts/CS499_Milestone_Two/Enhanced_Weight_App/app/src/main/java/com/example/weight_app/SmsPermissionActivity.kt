package com.example.weight_app

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Permission prompts stay in the Activity; sending is handled by GoalNotifier. */
class SmsPermissionActivity : ComponentActivity() {
    private var userId = -1
    private lateinit var status: TextView
    private lateinit var number: EditText
    private val prefs get() = getSharedPreferences("sms_user_$userId", MODE_PRIVATE)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else {
            prefs.edit().putBoolean("enabled", false).apply()
            status.text = "Permission denied. Weight tracking still works."
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getIntExtra(DataGridActivity.USER_ID_EXTRA, -1)
        if (userId <= 0) { finish(); return }
        setContentView(R.layout.activity_sms_permission)
        status = findViewById(R.id.textViewSmsStatus)
        number = findViewById(R.id.editSmsNumber)
        if (savedInstanceState == null) number.setText(prefs.getString("number", ""))
        status.text = if (prefs.getBoolean("enabled", false) && GoalNotifier.hasPermission(this))
            "SMS is enabled for this account." else "SMS is off. Weight tracking still works."
        findViewById<Button>(R.id.buttonEnableSms).setOnClickListener {
            if (!GoalNotifier.validNumber(number.text.toString().trim())) {
                status.text = "Enter 4 to 15 digits, optionally starting with +."
            } else if (GoalNotifier.hasPermission(this)) enable()
            else permission.launch(Manifest.permission.SEND_SMS)
        }
        findViewById<Button>(R.id.buttonDisableSms).setOnClickListener {
            prefs.edit().putBoolean("enabled", false).apply()
            status.text = "SMS is off. Weight tracking still works."
        }
        findViewById<Button>(R.id.buttonBackToGrid).setOnClickListener { finish() }
    }
    private fun enable() {
        val destination = number.text.toString().trim()
        if (!GoalNotifier.validNumber(destination)) { status.text = "Enter a valid phone number."; return }
        prefs.edit().putString("number", destination).putBoolean("enabled", true).apply()
        status.text = "SMS enabled. Messages may incur carrier charges."
    }
}

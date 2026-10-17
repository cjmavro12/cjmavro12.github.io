package com.example.weight_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class SmsPermissionActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableBtn: Button
    private lateinit var backBtn: Button

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            val prefs = getSharedPreferences("sms_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("sms_enabled", isGranted).apply()

            statusText.text = if (isGranted) {
                "Permission granted. SMS notifications enabled."
            } else {
                "Permission denied. App works without SMS."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_permission)

        statusText = findViewById(R.id.textViewSmsStatus)
        enableBtn = findViewById(R.id.buttonEnableSms)
        backBtn = findViewById(R.id.buttonBackToGrid)

        val prefs = getSharedPreferences("sms_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("sms_enabled", false)

        statusText.text = if (enabled && hasSmsPermission(this)) {
            "SMS notifications enabled."
        } else {
            "SMS notifications are not enabled."
        }

        enableBtn.setOnClickListener {
            if (hasSmsPermission(this)) {
                prefs.edit().putBoolean("sms_enabled", true).apply()
                statusText.text = "SMS notifications enabled."
                Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }

        backBtn.setOnClickListener { finish() }
    }

    companion object {
        private const val DEFAULT_EMULATOR_NUMBER = "5554"

        fun hasSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun sendGoalReachedSmsIfAllowed(context: Context, message: String) {
            val prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
            val smsEnabled = prefs.getBoolean("sms_enabled", false)

            if (!smsEnabled) return
            if (!hasSmsPermission(context)) return

            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(DEFAULT_EMULATOR_NUMBER, null, message, null, null)
                Toast.makeText(context, "Goal SMS sent (emulator).", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

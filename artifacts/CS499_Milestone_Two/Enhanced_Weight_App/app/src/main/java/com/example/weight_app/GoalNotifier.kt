package com.example.weight_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Optional side effect, kept separate from screens and database operations. */
class GoalNotifier(private val context: Context) {
    fun notify(userId: Int): String {
        val prefs = context.getSharedPreferences("sms_user_$userId", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return " Goal reached."
        if (!hasPermission(context)) return " Goal reached; SMS permission is off."
        val number = prefs.getString("number", "").orEmpty()
        if (!validNumber(number)) return " Goal reached; check SMS settings."
        return try {
            val manager = context.getSystemService(SmsManager::class.java)
                ?: return " Goal reached; SMS unavailable."
            // Do not include private weight values in the outgoing message.
            manager.sendTextMessage(number, null, "You reached your weight tracking goal.", null, null)
            " Goal reached; SMS request submitted."
        } catch (_: SecurityException) {
            " Goal reached; SMS permission unavailable."
        } catch (_: IllegalArgumentException) {
            " Goal reached; SMS settings could not be used."
        } catch (_: UnsupportedOperationException) {
            " Goal reached; this device cannot send SMS."
        } catch (_: IllegalStateException) {
            " Goal reached; SMS service unavailable."
        }
    }
    companion object {
        fun validNumber(value: String) = value.matches(Regex("\\+?[0-9]{4,15}"))
        fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
}

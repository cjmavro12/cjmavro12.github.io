package com.example.weight_app

import java.time.LocalDate
import java.time.format.DateTimeParseException

object InputValidator {
    fun date(value: String): Boolean = try {
        value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && LocalDate.parse(value).year in 1..9999
    } catch (_: DateTimeParseException) { false }
    fun weight(value: Double?): Boolean = value != null && value.isFinite() && value in 1.0..2000.0
    fun entry(dateText: String, value: Double) {
        require(date(dateText)) { "Use a valid date in YYYY-MM-DD format." }
        require(weight(value)) { "Enter a weight between 1 and 2000." }
    }
    fun credentials(username: String, password: String) {
        require(username.isNotBlank() && username.length <= 80) { "Enter a username of 1 to 80 characters." }
        require(password.isNotEmpty() && password.length <= 128) { "Enter a password of 1 to 128 characters." }
    }
}

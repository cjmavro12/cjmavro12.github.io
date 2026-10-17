package com.example.weight_app

import org.junit.Assert.*
import org.junit.Test

class EnhancementUnitTest {
    @Test fun datesRejectImpossibleDays() {
        assertTrue(InputValidator.date("2024-02-29"))
        assertFalse(InputValidator.date("2025-02-29"))
        assertFalse(InputValidator.date("2026-13-01"))
        assertFalse(InputValidator.date("09/20/2026"))
        assertFalse(InputValidator.date("0000-01-01"))
    }
    @Test fun weightsRejectNonFiniteAndOutOfRangeValues() {
        assertTrue(InputValidator.weight(1.0))
        assertTrue(InputValidator.weight(2000.0))
        for (value in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0, 2000.1))
            assertFalse(InputValidator.weight(value))
    }
    @Test fun saltsDifferAndVerificationWorks() {
        val first = PasswordHasher.hash("Student test password")
        val second = PasswordHasher.hash("Student test password")
        assertNotEquals(first, second)
        assertFalse(first.contains("Student test password"))
        assertTrue(PasswordHasher.verify("Student test password", first))
        assertFalse(PasswordHasher.verify("wrong", first))
        assertFalse(PasswordHasher.verify("anything", "malformed"))
        assertFalse(PasswordHasher.verify("anything", "pbkdf2-sha256\$999999999\$bad\$bad"))
    }
    @Test fun listDiffDistinguishesIdentityFromContent() {
        val first = WeightEntry(1, "2026-09-20", 190.0)
        val edited = first.copy(weight = 189.0)
        assertTrue(WeightAdapter.DIFF.areItemsTheSame(first, edited))
        assertFalse(WeightAdapter.DIFF.areContentsTheSame(first, edited))
        assertTrue(WeightAdapter.DIFF.areContentsTheSame(first, first.copy()))
        assertFalse(WeightAdapter.DIFF.areItemsTheSame(first, first.copy(id = 2)))
    }
}

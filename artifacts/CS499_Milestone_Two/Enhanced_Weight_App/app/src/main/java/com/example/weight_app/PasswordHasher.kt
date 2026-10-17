package com.example.weight_app

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Versioned PBKDF2 record; never store or log the input password. */
object PasswordHasher {
    private const val ITERATIONS = 600_000
    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encoder = Base64.getEncoder()
        return listOf("pbkdf2-sha256", ITERATIONS.toString(), encoder.encodeToString(salt),
            encoder.encodeToString(derive(password, salt, ITERATIONS))).joinToString("$")
    }
    fun verify(password: String, record: String): Boolean {
        return try {
            val parts = record.split('$')
            if (parts.size != 4 || parts[0] != "pbkdf2-sha256") return false
            val rounds = parts[1].toIntOrNull() ?: return false
            if (rounds !in 600_000..1_200_000) return false
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            if (salt.size != 16 || expected.size != 32) return false
            MessageDigest.isEqual(expected, derive(password, salt, rounds))
        } catch (_: IllegalArgumentException) { false }
    }
}

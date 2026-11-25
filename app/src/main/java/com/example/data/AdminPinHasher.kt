package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object AdminPinHasher {
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6
    private const val SALT_BYTES = 16
    private const val HASH_ITERATIONS = 10_000

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun createSaltAndHash(pin: String): Pair<String, String> {
        require(isValidPinFormat(pin)) { "Admin PIN must be 4 to 6 digits." }
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = computeIteratedHash(pin, salt)
        return salt.toHexString() to hash.toHexString()
    }

    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        val salt = hexToByteArrayOrNull(saltHex) ?: return false
        val expectedHash = hexToByteArrayOrNull(expectedHashHex) ?: return false
        if (salt.isEmpty() || expectedHash.isEmpty()) return false
        val actualHash = computeIteratedHash(pin, salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    private fun computeIteratedHash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        digest.update(salt)
        var current = digest.digest(pinBytes)
        repeat(HASH_ITERATIONS - 1) {
            digest.reset()
            digest.update(salt)
            current = digest.digest(current)
        }
        return current
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4])
            sb.append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    private fun hexToByteArrayOrNull(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hex[i * 2].digitToIntOrNull(16) ?: return null
            val lo = hex[i * 2 + 1].digitToIntOrNull(16) ?: return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private const val HEX_CHARS = "0123456789abcdef"
}

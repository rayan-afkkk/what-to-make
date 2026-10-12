package com.whattocook.app.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Handles secure salting and hashing of user passwords before they are stored.
 * Passwords are never sent or stored in plaintext to prevent unauthorized data exposure.
 */
object PasswordHasher {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt.toHex()
    }

    fun hash(password: String, saltHex: String): String {
        val salt = saltHex.fromHex()
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val skf = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        val hash = skf.generateSecret(spec).encoded
        return hash.toHex()
    }

    fun verify(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val calculated = hash(password, saltHex)
        return MessageDigest.isEqual(calculated.toByteArray(), expectedHashHex.toByteArray())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val high = Character.digit(this[i], 16)
            val low = Character.digit(this[i + 1], 16)
            data[i / 2] = ((high shl 4) + low).toByte()
            i += 2
        }
        return data
    }
}

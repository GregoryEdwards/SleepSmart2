package com.sleepsmart.app.data.db

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Generates a 256-bit random passphrase, encrypts it with an AES-GCM key
 * stored in the Android Keystore, and persists the ciphertext+IV in shared
 * prefs. The plaintext passphrase is decrypted at runtime to unlock SQLCipher.
 *
 * If decryption fails (corrupt prefs, factory-reset keystore), we treat the
 * database as unrecoverable and rotate to a fresh key — the user is warned
 * via UI on first cold start when this happens (see [wasRotated]).
 */
@Singleton
class DatabaseKey @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sleepsmart_keys", Context.MODE_PRIVATE)

    @Volatile private var rotated: Boolean = false
    val wasRotated: Boolean get() = rotated

    fun passphrase(): ByteArray {
        ensureKeystoreKey()
        val ct = prefs.getString(PREF_CT, null)
        val iv = prefs.getString(PREF_IV, null)
        if (ct != null && iv != null) {
            runCatching { return decrypt(ct.fromB64(), iv.fromB64()) }
                .onFailure { rotated = true }
        }
        // Generate a fresh passphrase
        val pass = ByteArray(32).also { Random.nextBytes(it) }
        val (newCt, newIv) = encrypt(pass)
        prefs.edit {
            putString(PREF_CT, newCt.toB64())
            putString(PREF_IV, newIv.toB64())
        }
        return pass
    }

    private fun ensureKeystoreKey() {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        kg.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        kg.generateKey()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ct = cipher.doFinal(plain)
        return ct to cipher.iv
    }

    private fun decrypt(ct: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    private fun ByteArray.toB64() = android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
    private fun String.fromB64() = android.util.Base64.decode(this, android.util.Base64.NO_WRAP)

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "sleepsmart_db_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREF_CT = "db_pass_ct"
        private const val PREF_IV = "db_pass_iv"
    }
}

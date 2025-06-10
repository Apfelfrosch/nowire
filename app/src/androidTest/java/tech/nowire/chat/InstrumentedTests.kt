package tech.nowire.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.nowire.chat.hcl.LibSodiumUtils
import tech.nowire.chat.hcl.Ratchet

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTests {

    @Test
    fun testBase64() {
        val k = LibSodiumUtils.stringToSymmetricKey("hallo", "fisch").get()
        val b64_encoded = LibSodiumUtils.toBase64(k).get()
        val b64_decoded = LibSodiumUtils.toBytes(b64_encoded).get()

        assertFalse(k.contentEquals(b64_encoded.encodeToByteArray()))
        assertFalse(b64_encoded.encodeToByteArray().contentEquals(b64_decoded))
        assertEquals(32, k.size)
        assertTrue(k.contentEquals(b64_decoded))
    }

    @Test
    fun test_without_nonce_key_encrypt_decrypt() {
        val key = LibSodiumUtils.generateSymmetricKey();
        val msg = "hallo ich bin eine äääöö\$§§ test nachricht "
        val encrypted = LibSodiumUtils.stringSymmetricEncrypt(key, msg).get()
        val decrypted = LibSodiumUtils.stringSymmetricDecrypt(key, encrypted).get()
        assertFalse(msg.encodeToByteArray().contentEquals(encrypted))
        assertEquals(msg, decrypted)
    }

    @Test
    fun testSign() {
        val kp1 = LibSodiumUtils.generateSignKeyPair()
        val otherPk = LibSodiumUtils.generateSignKeyPair().signPublicKey

        val msg = "cmd\\npayÄ§load"
        val signed = LibSodiumUtils.signString(msg, kp1.signSecretKey).get()

        assertFalse(msg.encodeToByteArray().contentEquals(signed))
        assertTrue(LibSodiumUtils.signOpen(signed, otherPk).isEmpty)
        assertTrue(LibSodiumUtils.signOpen(signed, kp1.signPublicKey).get().contentEquals(msg.encodeToByteArray()))
    }

    @Test
    fun testDoubleRatchet() {
        val key = LibSodiumUtils.generateSymmetricKey()
        val rootRatchet = Ratchet(key)

        val k1 = rootRatchet.next().get().symmetricKey
        val k2 = rootRatchet.next().get().symmetricKey

        assertFalse(k1.contentEquals(k2))

        val a_send = Ratchet(k1)
        val a_recv = Ratchet(k2)

        val b_send = Ratchet(k2)
        val b_recv = Ratchet(k1)

        for (msg in arrayOf("Hallo ÄÄ ich bin einßß??ß Fisc§§h!!  ")) {
            assertEquals(
                msg,
                b_recv.decryptString(
                    a_send.encryptString(msg).get()
                ).get()
            )

            assertEquals(
                msg,
                a_recv.decryptString(
                    b_send.encryptString(msg).get()
                ).get()
            )
        }
    }

    @Test
    fun testDetachedSignatures() {
        val msg = "Hallo";

        val kp1 = LibSodiumUtils.generateSignKeyPair();
        val otherPk = LibSodiumUtils.generateSignKeyPair().signPublicKey;

        val sig = LibSodiumUtils.signStringDetached(msg, kp1.signSecretKey).get()
        assertTrue(sig.isNotEmpty())
        assertTrue(LibSodiumUtils.signDetachedVerify(msg.encodeToByteArray(), sig, kp1.signPublicKey));
        assertFalse(LibSodiumUtils.signDetachedVerify(msg.encodeToByteArray(), sig, otherPk));
    }
}
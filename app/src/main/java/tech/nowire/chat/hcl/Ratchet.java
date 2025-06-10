package tech.nowire.chat.hcl;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.NativeLongByReference;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public final class Ratchet {

    private byte[] currentKey; // len: 32

    public Ratchet(byte[] currentKey) {
        this.currentKey = currentKey;
    }

    public Optional<RatchetResult> next() {
        JnaSodium s = LibSodium.getInstance();

        final int expandBufSize = 88;
        byte[] buf = new byte[expandBufSize];

        int expandResult = s.crypto_kdf_hkdf_sha256_expand(
                buf,
                new NativeLong(buf.length),
                null,
                new NativeLong(0),
                this.currentKey
        );

        if (expandResult != 0) {
            return Optional.empty();
        }

        byte[] newRatchetKey = new byte[JnaSodium.SYMMETRIC_KEY_BYTES];
        byte[] resultKey = new byte[JnaSodium.SYMMETRIC_KEY_BYTES];
        byte[] resultNonce = new byte[JnaSodium.NONCE_BYTES];

        System.arraycopy(buf, 0, newRatchetKey, 0, newRatchetKey.length);
        System.arraycopy(buf, newRatchetKey.length, resultKey, 0, resultKey.length);
        System.arraycopy(buf, newRatchetKey.length + resultKey.length, resultNonce, 0, resultNonce.length);

        this.currentKey = newRatchetKey;

        return Optional.of(new RatchetResult(resultKey, resultNonce));
    }

    public Optional<byte[]> encryptString(String str) {
        byte[] sBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] paddedBuf = new byte[sBytes.length + JnaSodium.BLOCK_SIZE];
        System.arraycopy(sBytes,0, paddedBuf, 0, sBytes.length);
        NativeLongByReference paddedBufLen = new NativeLongByReference();

        JnaSodium s = LibSodium.getInstance();
        int padResult = s.sodium_pad(
                paddedBufLen,
                paddedBuf,
                new NativeLong(sBytes.length),
                new NativeLong(JnaSodium.BLOCK_SIZE),
                new NativeLong(paddedBuf.length)
        );

        if (padResult != 0) {
            return Optional.empty();
        }

        byte[] bytesToEncrypt = new byte[paddedBufLen.getValue().intValue()];
        System.arraycopy(paddedBuf, 0, bytesToEncrypt, 0, bytesToEncrypt.length);

        Optional<RatchetResult> optionalRatchetResult = this.next();
        if (optionalRatchetResult.isEmpty()) {
            return Optional.empty();
        }
        RatchetResult ratchetResult = optionalRatchetResult.get();

        int ciphertextLen = bytesToEncrypt.length + JnaSodium.MAC_BYTES;
        byte[] ciphertext = new byte[ciphertextLen];

        if (s.crypto_secretbox_easy(
                ciphertext,
                bytesToEncrypt,
                bytesToEncrypt.length,
                ratchetResult.nonce,
                ratchetResult.symmetricKey
        ) != 0) {
            return Optional.empty();
        }

        return Optional.of(ciphertext);
    }

    public Optional<String> decryptString(byte[] ciphertext) {
        Optional<RatchetResult> optionalRatchetResult = this.next();
        if (optionalRatchetResult.isEmpty()) {
            return Optional.empty();
        }
        RatchetResult ratchetResult = optionalRatchetResult.get();

        JnaSodium s = LibSodium.getInstance();

        int msgLen = ciphertext.length - JnaSodium.MAC_BYTES;
        byte[] buf = new byte[msgLen];
        if (s.crypto_secretbox_open_easy(
                buf,
                ciphertext,
                ciphertext.length,
                ratchetResult.nonce,
                ratchetResult.symmetricKey
        ) != 0) {
            return Optional.empty();
        }

        NativeLongByReference unpaddedLen = new NativeLongByReference();
        if (s.sodium_unpad(
                unpaddedLen,
                buf,
                new NativeLong(buf.length),
                new NativeLong(JnaSodium.BLOCK_SIZE)
        ) != 0) {
            return Optional.empty();
        }

        byte[] resultBuf = new byte[unpaddedLen.getValue().intValue()];
        System.arraycopy(buf, 0, resultBuf, 0, resultBuf.length);
        return Optional.of(new String(resultBuf, StandardCharsets.UTF_8));
    }

    public byte[] getCurrentKey() {
        return Arrays.copyOf(currentKey, currentKey.length);
    }

}

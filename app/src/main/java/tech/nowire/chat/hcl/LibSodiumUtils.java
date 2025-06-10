package tech.nowire.chat.hcl;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class LibSodiumUtils {

    private LibSodiumUtils() {}

    public static byte[] randomBuf(int len) {
        byte[] buf = new byte[len];
        LibSodium.getInstance().randombytes_buf(buf, new NativeLong(buf.length));
        return buf;
    }

    public static int randomUniform(int upperBound) {
        return LibSodium.getInstance().randombytes_uniform(upperBound);
    }

    public static byte[] generateSymmetricKey() {
        byte[] buf = new byte[JnaSodium.SYMMETRIC_KEY_BYTES];
        LibSodium.getInstance().crypto_secretbox_keygen(buf);
        return buf;
    }

    public static Optional<byte[]> signBytes(byte[] toSign, byte[] secretKey /* len: sign secret key */) {
        int signBufLen = toSign.length + JnaSodium.SIGN_BYTES;
        byte[] signBuf = new byte[signBufLen];
        LongByReference result_len = new LongByReference();

        if (LibSodium.getInstance().crypto_sign(
                signBuf,
                result_len,
                toSign,
                toSign.length,
                secretKey
        ) != 0) {
            return Optional.empty();
        }

        byte[] result = new byte[(int) result_len.getValue()];
        System.arraycopy(signBuf, 0, result, 0, result.length);

        return Optional.of(result);
    }

    public static Optional<byte[]> signString(String str, byte[] secretKey /* len: sign secret key */) {
        return signBytes(str.getBytes(StandardCharsets.UTF_8), secretKey);
    }

    public static Optional<byte[]> signOpen(byte[] signedBytes, byte[] publicKey /* len: sign public key */) {
        int unsignedMessageCap = signedBytes.length - JnaSodium.SIGN_BYTES;
        byte[] unsignedMessageBuf = new byte[unsignedMessageCap];
        LongByReference resultLen = new LongByReference();

        if (LibSodium.getInstance().crypto_sign_open(
                unsignedMessageBuf,
                resultLen,
                signedBytes,
                signedBytes.length,
                publicKey
        ) != 0) {
            return Optional.empty();
        }

        byte[] result = new byte[(int) resultLen.getValue()];
        System.arraycopy(unsignedMessageBuf, 0, result, 0, result.length);
        return Optional.of(result);
    }

    public static SignKeyPair generateSignKeyPair() {
        byte[] pk = new byte[JnaSodium.SIGN_PUBLIC_KEY_BYTES];
        byte[] sk = new byte[JnaSodium.SIGN_SECRET_KEY_BYTES];
        LibSodium.getInstance().crypto_sign_keypair(pk, sk);
        return new SignKeyPair(pk, sk);
    }

    public static Optional<byte[]> signStringDetached(String s, byte[] sk /* len: sign secret key */) {
        byte[] result = new byte[JnaSodium.SIGN_BYTES];
        byte[] sBytes = s.getBytes(StandardCharsets.UTF_8);

        if (LibSodium.getInstance().crypto_sign_detached(
                result,
                null,
                sBytes,
                sBytes.length,
                sk
        ) != 0) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public static boolean signDetachedVerify(byte[] message, byte[] signature, byte[] pk /* len: sign public key */) {
        return LibSodium.getInstance().crypto_sign_verify_detached(
                signature,
                message,
                message.length,
                pk
        ) == 0;
    }

    public static Optional<byte[]> stringToSymmetricKey(String s, String salt) {
        byte[] result = new byte[JnaSodium.SYMMETRIC_KEY_BYTES];

        if (LibSodium.getInstance().crypto_kdf_hkdf_sha256_extract(
                result,
                salt,
                new NativeLong(salt.getBytes(StandardCharsets.UTF_8).length),
                s,
                new NativeLong(s.getBytes(StandardCharsets.UTF_8).length)
        ) != 0) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public static Optional<byte[]> stringSymmetricEncrypt(byte[] key /* len: symmetric key */, String s) {
        return new Ratchet(key).encryptString(s);
    }

    public static Optional<String> stringSymmetricDecrypt(byte[] key /* len: symmetric key */, byte[] c) {
        return new Ratchet(key).decryptString(c);
    }

    public static Optional<String> toBase64(byte[] bytes) {
        byte[] b64 = new byte[bytes.length * 4];
        if (LibSodium.getInstance().sodium_bin2base64(
                b64,
                new NativeLong(b64.length),
                bytes,
                new NativeLong(bytes.length),
                JnaSodium.BASE64_VARIANT_ORIGINAL
        ) == null) {
            return Optional.empty();
        }
        return Optional.of(Native.toString(b64));
    }

    public static Optional<byte[]> toBytes(String b64String) {
        byte[] b64 = b64String.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[b64.length / 4 * 3];

        NativeLongByReference actualBinLen = new NativeLongByReference();

        if (LibSodium.getInstance().sodium_base642bin(
                bytes,
                new NativeLong(bytes.length),
                b64String,
                new NativeLong(b64.length),
                null,
                actualBinLen,
                null,
                JnaSodium.BASE64_VARIANT_ORIGINAL
        ) != 0) {
            return Optional.empty();
        }

        byte[] result = new byte[(int) actualBinLen.getValue().intValue()];
        System.arraycopy(bytes, 0, result, 0, result.length);
        return Optional.of(result);
    }

}

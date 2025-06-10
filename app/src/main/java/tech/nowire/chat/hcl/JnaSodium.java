package tech.nowire.chat.hcl;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;

public interface JnaSodium extends Library {

    int BASE64_VARIANT_ORIGINAL = 1;

    int SYMMETRIC_KEY_BYTES = 32;
    int NONCE_BYTES = 24;
    int MAC_BYTES = 16;

    int PUBLIC_KEY_BYTES = 32;
    int SECRET_KEY_BYTES = 32;

    int SIGN_PUBLIC_KEY_BYTES = 32;
    int SIGN_SECRET_KEY_BYTES = 32 + 32;
    int SIGN_BYTES = 64;

    int BLOCK_SIZE = 16;

    int sodium_init();

    int randombytes_uniform(int upper_bound);

    void randombytes_buf(byte[] buf, NativeLong len);

    Pointer sodium_bin2base64(byte[] b64, NativeLong b64_max_len, byte[] bin, NativeLong bin_len, int variant);

    int sodium_base642bin(
            byte[] bin, NativeLong bin_maxlen,
            String b64, NativeLong b64_len,
            String ignore,
            NativeLongByReference bin_len,
            Pointer b64_end,
            int variant
    );

    int crypto_kdf_hkdf_sha256_expand(
            byte[] out,
            NativeLong out_len,
            String ctx,
            NativeLong ctx_len,
            byte[] prk // fixed len: crypto_kdf_hkdf_sha256_KEYBYTES
    );

    int crypto_kdf_hkdf_sha256_extract(
            byte[] prk, // fixed len: crypto_kdf_hkdf_sha256_KEYBYTES
            String salt, NativeLong salt_len,
            String ikm, NativeLong ikm_len
    );

    int crypto_secretbox_easy(
            byte[] c, // out: ciphertext (len: mlen + crypto_secretbox_MACBYTES)
            byte[] m, // in: message
            long mlen,
            byte[] n, // in: nonce
            byte[] k // in: symmetric key
    );

    int crypto_secretbox_open_easy(
            byte[] m, // out: message
            byte[] c, // in: ciphertext
            long clen,
            byte[] n, // in: nonce
            byte[] k // in: symmetric key
    );

    void crypto_secretbox_keygen(
            byte[] out // len: symmetric key bytes
    );

    int crypto_box_keypair(
            byte[] pk, // len: public key bytes
            byte[] sk // len: secret key bytes
    );

    int crypto_sign_keypair(
            byte[] pk, // len: sign public key bytes
            byte[] sk // len: sign secret key bytes
    );

    int crypto_sign(
            byte[] sm, // out: signed message
            LongByReference smlen_p, // out: signed message actual length
            byte[] m, // in: message
            long mlen,
            byte[] sk // in: sign secret key
    );

    int crypto_sign_open(
            byte[] m, // out: message without signature
            LongByReference mlen_p, // out: message without signature length
            byte[] sm, // in: singed message
            long smlen,
            byte[] pk // in: sign public key
    );

    int crypto_sign_detached(
            byte[] sig, //out: signature for the message
            LongByReference siglen_p, // out: actual length of signature
            byte[] m, // in: message to create signature for
            long mlen,
            byte[] sk // in: sign secret key
    );

    int crypto_sign_verify_detached(
            byte[] sig, // in: the supposed signature
            byte[] m, // in: the message that the signature is supposedly for
            long mlen,
            byte[] pk // in: public key that the signature was supposedly created with
    );

    int sodium_pad(
            NativeLongByReference padded_buflen_p, // out: actual length of the now padded result
            byte[] buf, // out: the buf to add padding to
            NativeLong unpadded_buflen,
            NativeLong blocksize,
            NativeLong max_buflen
    );

    int sodium_unpad(
            NativeLongByReference unpadded_buflen_p, // out: length with padding removed
            byte[] buf, // in: the padded buffer
            NativeLong padded_buflen,
            NativeLong blocksize
    );

}

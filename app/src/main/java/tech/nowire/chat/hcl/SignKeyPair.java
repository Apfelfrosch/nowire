package tech.nowire.chat.hcl;

public final class SignKeyPair {

    public final byte[] signPublicKey;
    public final byte[] signSecretKey;

    public SignKeyPair(byte[] signPublicKey, byte[] signSecretKey) {
        this.signPublicKey = signPublicKey;
        this.signSecretKey = signSecretKey;
    }

}

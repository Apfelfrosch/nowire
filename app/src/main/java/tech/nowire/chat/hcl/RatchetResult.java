package tech.nowire.chat.hcl;

public final class RatchetResult {

    public final byte[] symmetricKey;
    public final byte[] nonce;

    public RatchetResult(byte[] symmetricKey, byte[] nonce) {
        this.symmetricKey = symmetricKey;
        this.nonce = nonce;
    }

}

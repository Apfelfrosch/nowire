package tech.nowire.chat.hcl;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.Collections;

public final class LibSodium {

    private static JnaSodium instance = null;

    private LibSodium() {}

    public static JnaSodium getInstance() {
        if (instance == null) {
            JnaSodium sodium = Native.load("sodium", JnaSodium.class, Collections.singletonMap(Library.OPTION_STRING_ENCODING, "UTF-8"));

            if (sodium.sodium_init() < 0) {
                throw new RuntimeException("Couldn't init sodium");
            }

            instance = sodium;
        }

        return instance;
    }
}

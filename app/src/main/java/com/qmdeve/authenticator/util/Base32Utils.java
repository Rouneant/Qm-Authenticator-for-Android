package com.qmdeve.authenticator.util;

import java.util.HashMap;
import java.util.Map;

public class Base32Utils {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final Map<Character, Integer> BASE32_MAP = new HashMap<>();

    static {
        for (int i = 0; i < BASE32_CHARS.length(); i++) {
            BASE32_MAP.put(BASE32_CHARS.charAt(i), i);
        }
    }

    public static byte[] decode(String base32) {
        base32 = base32.toUpperCase().replace("=", "");
        int length = base32.length();
        int outputLength = (length * 5) / 8;
        byte[] output = new byte[outputLength];

        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (int i = 0; i < length; i++) {
            char c = base32.charAt(i);
            if (!BASE32_MAP.containsKey(c)) {
                continue;
            }
            buffer <<= 5;
            buffer |= BASE32_MAP.get(c) & 0x1F;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return output;
    }
}
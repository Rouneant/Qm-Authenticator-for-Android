package com.qmdeve.authenticator.util;

import java.util.HashMap;
import java.util.Map;

public final class Base32Utils {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final Map<Character, Integer> BASE32_MAP = new HashMap<>();

    static {
        for (int i = 0; i < BASE32_CHARS.length(); i++) {
            BASE32_MAP.put(BASE32_CHARS.charAt(i), i);
        }
    }

    private Base32Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] decode(String base32) {
        if (base32 == null) {
            return new byte[0];
        }
        String normalized = base32.toUpperCase().replace("=", "");
        int outputLength = (normalized.length() * 5) / 8;
        byte[] output = new byte[outputLength == 0 ? 1 : outputLength];

        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            Integer value = BASE32_MAP.get(c);
            if (value == null) {
                continue;
            }
            buffer = (buffer << 5) | (value & 0x1F);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }

        byte[] result = new byte[count];
        System.arraycopy(output, 0, result, 0, count);
        return result;
    }

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                int index = (buffer >>> (bitsLeft - 5)) & 0x1F;
                result.append(BASE32_CHARS.charAt(index));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_CHARS.charAt(index));
        }

        return result.toString();
    }
}
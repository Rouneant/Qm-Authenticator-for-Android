package com.qmdeve.authenticator.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TOTPGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int[] DIGITS_POWER = {
            1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000
    };

    public static String generateTOTP(String base32Key, int timeStep, int digits) {
        long time = System.currentTimeMillis() / 1000;
        long timeCounter = time / timeStep;

        byte[] key = Base32Utils.decode(base32Key);
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(timeCounter).array();

        try {
            byte[] hash = hmacSha1(key, counterBytes);
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);
            int otp = binary % DIGITS_POWER[digits];
            return String.format("%0" + digits + "d", otp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] hmacSha1(byte[] key, byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec spec = new SecretKeySpec(key, HMAC_ALGORITHM);
        mac.init(spec);
        return mac.doFinal(data);
    }
}
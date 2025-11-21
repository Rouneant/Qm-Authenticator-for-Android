package com.qmdeve.authenticator.util;

import android.text.TextUtils;

import com.qmdeve.authenticator.model.Token;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TOTPGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int MIN_DIGITS = 4;
    private static final int MAX_DIGITS = 8;

    private TOTPGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateTOTP(String base32Key, int timeStep, int digits) {
        if (TextUtils.isEmpty(base32Key)) {
            return null;
        }
        int safeDigits = clampDigits(digits);
        int safeTimeStep = Math.max(timeStep, 1);

        byte[] key = Base32Utils.decode(base32Key);
        if (key.length == 0) {
            return null;
        }

        long timeCounter = (System.currentTimeMillis() / 1000) / safeTimeStep;
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(timeCounter).array();

        try {
            byte[] hash = hmacSha1(key, counterBytes);
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % pow10(safeDigits);
            return String.format(Locale.US, "%0" + safeDigits + "d", otp);
        } catch (Exception e) {
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

    private static int clampDigits(int digits) {
        if (digits < MIN_DIGITS || digits > MAX_DIGITS) {
            return Token.DEFAULT_DIGITS;
        }
        return digits;
    }

    private static int pow10(int exp) {
        int result = 1;
        for (int i = 0; i < exp; i++) {
            result *= 10;
        }
        return result;
    }
}
package com.qmdeve.authenticator.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;
import java.util.UUID;

@Entity(tableName = "tokens")
public class Token {

    public static final int DEFAULT_DIGITS = 6;
    public static final int DEFAULT_PERIOD = 30;
    public static final int MIN_DIGITS = 4;
    public static final int MAX_DIGITS = 8;
    public static final int MIN_PERIOD = 10;
    public static final int MAX_PERIOD = 120;

    @PrimaryKey
    @NonNull
    private String id;
    private String issuer;
    private String account;
    private String secret;
    private TokenType type;
    private int digits;
    private int period;
    private long counter;

    public enum TokenType {
        TOTP
    }

    public Token() {
        this.id = UUID.randomUUID().toString();
        this.type = TokenType.TOTP;
        this.digits = DEFAULT_DIGITS;
        this.period = DEFAULT_PERIOD;
        this.counter = 0;
    }

    @Ignore
    public Token(String issuer, String account, String secret, TokenType type) {
        this();
        this.issuer = issuer;
        this.account = account;
        this.secret = secret;
        this.type = type;
        normalize();
    }

    /**
     * 统一清洗数据：补全 ID、规范秘钥大小写以及限制位数周期范围。
     */
    public void normalize() {
        if (id.trim().isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        issuer = trimToNull(issuer);
        account = account == null ? "" : account.trim();
        secret = secret == null ? "" : secret.replaceAll("\\s+", "").toUpperCase(Locale.US);
        if (type == null) {
            type = TokenType.TOTP;
        }
        digits = clamp(digits, MIN_DIGITS, MAX_DIGITS, DEFAULT_DIGITS);
        period = clamp(period, MIN_PERIOD, MAX_PERIOD, DEFAULT_PERIOD);
        if (counter < 0) {
            counter = 0;
        }
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public String getLabel() {
        if (issuer != null && !issuer.isEmpty()) {
            return issuer + " (" + account + ")";
        }
        return account;
    }

    private static int clamp(int value, int min, int max, int defaultValue) {
        if (value < min || value > max) {
            return defaultValue;
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
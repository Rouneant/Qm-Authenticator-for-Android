package com.qmdeve.authenticator.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.UUID;

@Entity(tableName = "tokens")
public class Token {
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
    }

    @Ignore
    public Token(String issuer, String account, String secret, TokenType type) {
        this.id = UUID.randomUUID().toString();
        this.issuer = issuer;
        this.account = account;
        this.secret = secret;
        this.type = type;
        this.digits = 6;
        this.period = 30;
        this.counter = 0;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public TokenType getType() { return type; }
    public void setType(TokenType type) { this.type = type; }

    public int getDigits() { return digits; }
    public void setDigits(int digits) { this.digits = digits; }

    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }

    public long getCounter() { return counter; }
    public void setCounter(long counter) { this.counter = counter; }

    public String getLabel() {
        if (issuer != null && !issuer.isEmpty()) {
            return issuer + " (" + account + ")";
        }
        return account;
    }
}
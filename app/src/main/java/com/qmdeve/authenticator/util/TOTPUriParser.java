package com.qmdeve.authenticator.util;

import android.text.TextUtils;

import com.qmdeve.authenticator.model.Token;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class TOTPUriParser {

    private TOTPUriParser() {
        throw new IllegalStateException("Utility class");
    }

    public static Token parseTotpUri(String rawUri) throws IllegalArgumentException {
        try {
            URI uri = URI.create(rawUri.trim());
            ensureScheme(uri);
            ensureType(uri);

            String label = extractLabel(uri);
            LabelParts labelParts = splitLabel(label);
            Map<String, String> params = parseQueryParameters(uri.getQuery());

            String secret = params.get("secret");
            if (TextUtils.isEmpty(secret)) {
                throw new IllegalArgumentException("Missing secret parameter");
            }

            Token token = new Token();
            token.setType(Token.TokenType.TOTP);
            token.setSecret(secret.replace(" ", ""));
            token.setAccount(labelParts.account);

            String issuerParam = params.get("issuer");
            token.setIssuer(!TextUtils.isEmpty(issuerParam)
                    ? decode(issuerParam)
                    : labelParts.issuer);

            token.setDigits(parseInt(params.get("digits"), Token.DEFAULT_DIGITS, "digits"));
            token.setPeriod(parseInt(params.get("period"), Token.DEFAULT_PERIOD, "period"));
            token.setCounter(0);
            token.normalize();
            return token;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse TOTP URI: " + e.getMessage(), e);
        }
    }

    private static void ensureScheme(URI uri) {
        if (!"otpauth".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Invalid scheme: " + uri.getScheme());
        }
    }

    private static void ensureType(URI uri) {
        String host = uri.getHost();
        if (!"totp".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Only TOTP type is supported");
        }
    }

    private static String extractLabel(URI uri) {
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("Empty label");
        }
        String label = path.startsWith("/") ? path.substring(1) : path;
        if (TextUtils.isEmpty(label)) {
            throw new IllegalArgumentException("Empty label");
        }
        return label;
    }

    private static LabelParts splitLabel(String label) {
        String[] parts = label.split(":", 2);
        if (parts.length == 2) {
            return new LabelParts(decode(parts[1]), decode(parts[0]));
        }
        return new LabelParts(decode(label), null);
    }

    private static Map<String, String> parseQueryParameters(String query) {
        Map<String, String> params = new HashMap<>();
        if (TextUtils.isEmpty(query)) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String decode(String component) {
        try {
            return URLDecoder.decode(component, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return component;
        }
    }

    private static int parseInt(String value, int defaultValue, String field) {
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + " format: " + value, e);
        }
    }

    private static final class LabelParts {
        final String account;
        final String issuer;

        LabelParts(String account, String issuer) {
            this.account = account;
            this.issuer = issuer;
        }
    }
}
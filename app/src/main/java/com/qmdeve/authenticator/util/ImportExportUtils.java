package com.qmdeve.authenticator.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.qmdeve.authenticator.model.ExportData;
import com.qmdeve.authenticator.model.Token;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ImportExportUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private ImportExportUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String exportToJson(List<Token> tokens) {
        ExportData exportData = new ExportData(tokens);
        return GSON.toJson(exportData);
    }

    public static List<Token> importFromJson(String jsonString) throws IllegalArgumentException {
        try {
            Type exportDataType = new TypeToken<ExportData>() {
            }.getType();
            ExportData exportData = GSON.fromJson(jsonString, exportDataType);

            if (exportData == null || exportData.getTokens() == null) {
                throw new IllegalArgumentException("Invalid export data format");
            }

            List<Token> sanitized = new ArrayList<>();
            for (Token token : exportData.getTokens()) {
                if (token == null || TextUtils.isEmpty(token.getSecret()) || TextUtils.isEmpty(token.getAccount())) {
                    continue;
                }
                token.normalize();
                sanitized.add(token);
            }
            if (sanitized.isEmpty()) {
                throw new IllegalArgumentException("No valid tokens found");
            }
            return sanitized;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse export data: " + e.getMessage(), e);
        }
    }

    public static boolean validateExportData(String jsonString) {
        try {
            importFromJson(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
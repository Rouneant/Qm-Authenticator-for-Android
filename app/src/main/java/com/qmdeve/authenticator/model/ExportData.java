package com.qmdeve.authenticator.model;

import java.util.ArrayList;
import java.util.List;

public class ExportData {
    private String version = "1.0.0";
    private String appName = "Qm Authenticator";
    private long exportTime = System.currentTimeMillis();
    private List<Token> tokens = new ArrayList<>();

    public ExportData() {}

    public ExportData(List<Token> tokens) {
        setTokens(tokens);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getExportTime() {
        return exportTime;
    }

    public void setExportTime(long exportTime) {
        this.exportTime = exportTime;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens == null ? new ArrayList<>() : new ArrayList<>(tokens);
        for (Token token : this.tokens) {
            if (token != null) {
                token.normalize();
            }
        }
    }
}
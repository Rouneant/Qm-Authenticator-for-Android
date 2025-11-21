package com.qmdeve.authenticator.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitHubUpdateChecker {

    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/Rouneant/Qm-Authenticator-for-Android/releases/latest";
    private static final String USER_AGENT = "QmAuthenticator-Android";

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UpdateCheckListener {
        void onUpdateAvailable(String latestVersion, String downloadUrl, String releaseBody);

        void onError(Exception e);
    }

    public GitHubUpdateChecker(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void checkForUpdates(UpdateCheckListener listener) {
        executor.execute(() -> fetchLatestRelease(listener));
    }

    private void fetchLatestRelease(UpdateCheckListener listener) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    JSONObject releaseJson = new JSONObject(response.toString());
                    processReleaseData(releaseJson, listener);
                }
            } else {
                throw new IllegalStateException("Unexpected response: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            notifyError(listener, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void processReleaseData(JSONObject releaseJson, UpdateCheckListener listener) {
        try {
            String tagName = releaseJson.getString("tag_name");
            String releaseBody = releaseJson.optString("body", "");
            String downloadUrl = extractDownloadUrl(releaseJson);

            String currentVersion = getCurrentVersion();
            if (isNewVersion(currentVersion, tagName)) {
                notifySuccess(listener, tagName, downloadUrl, releaseBody);
            }
        } catch (Exception e) {
            notifyError(listener, e);
        }
    }

    private String extractDownloadUrl(JSONObject releaseJson) throws Exception {
        JSONArray assets = releaseJson.optJSONArray("assets");
        if (assets != null && assets.length() > 0) {
            JSONObject firstAsset = assets.getJSONObject(0);
            return firstAsset.getString("browser_download_url");
        }
        return releaseJson.getString("zipball_url");
    }

    private String getCurrentVersion() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionName;
    }

    private boolean isNewVersion(String currentVersion, String latestTag) {
        if (currentVersion == null) {
            return true;
        }
        String[] currentParts = cleanVersionString(currentVersion).split("\\.");
        String[] latestParts = cleanVersionString(latestTag).split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentNum = i < currentParts.length ? parseInt(currentParts[i]) : 0;
            int latestNum = i < latestParts.length ? parseInt(latestParts[i]) : 0;
            if (latestNum > currentNum) {
                return true;
            }
            if (latestNum < currentNum) {
                return false;
            }
        }
        return false;
    }

    private String cleanVersionString(String version) {
        return version == null ? "" : version.replace("v", "").replace("V", "").trim();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void notifySuccess(UpdateCheckListener listener, String version, String url, String body) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onUpdateAvailable(version, url, body));
    }

    private void notifyError(UpdateCheckListener listener, Exception e) {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> listener.onError(e));
    }

    public void destroy() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
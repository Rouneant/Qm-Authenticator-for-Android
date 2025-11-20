package com.qmdeve.authenticator.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitHubUpdateChecker {
    private final Context context;
    private UpdateCheckListener listener;
    private final ExecutorService executor;

    public interface UpdateCheckListener {
        void onUpdateAvailable(String latestVersion, String downloadUrl, String releaseBody);
        void onError(Exception e);
    }

    public GitHubUpdateChecker(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void checkForUpdates(UpdateCheckListener listener) {
        this.listener = listener;
        executor.execute(this::fetchLatestRelease);
    }

    private void fetchLatestRelease() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.github.com/repos/Rouneant/Qm-Authenticator-for-Android/releases/latest").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject releaseJson = new JSONObject(response.toString());
                processReleaseData(releaseJson);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void processReleaseData(JSONObject releaseJson) {
        try {
            String tagName = releaseJson.getString("tag_name");
            String releaseBody = releaseJson.optString("body", "");
            String downloadUrl = null;
            JSONArray assets = releaseJson.getJSONArray("assets");
            if (assets.length() > 0) {
                JSONObject firstAsset = assets.getJSONObject(0);
                downloadUrl = firstAsset.getString("browser_download_url");
            }

            if (downloadUrl == null) {
                downloadUrl = releaseJson.getString("zipball_url");
            }

            String currentVersion = getCurrentVersion();
            if (isNewVersion(currentVersion, tagName)) {
                if (listener != null) {
                    listener.onUpdateAvailable(tagName, downloadUrl, releaseBody);
                }
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    private String getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private boolean isNewVersion(String currentVersion, String latestTag) {
        return !cleanVersionString(currentVersion).equals(cleanVersionString(latestTag));
    }

    private String cleanVersionString(String version) {
        if (version == null) return "";
        return version.replace("v", "")
                .replace("V", "")
                .trim();
    }

    public void destroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
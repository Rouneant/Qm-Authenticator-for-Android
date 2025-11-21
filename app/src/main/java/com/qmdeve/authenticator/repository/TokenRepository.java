package com.qmdeve.authenticator.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.qmdeve.authenticator.data.AppDatabase;
import com.qmdeve.authenticator.data.dao.TokenDao;
import com.qmdeve.authenticator.model.Token;
import com.qmdeve.authenticator.util.ImportExportUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenRepository {

    private final TokenDao tokenDao;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TokenRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        tokenDao = database.tokenDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void getAllTokens(DataLoadCallback<List<Token>> callback) {
        executor.execute(() -> {
            try {
                List<Token> tokens = tokenDao.getAllTokens();
                postSuccess(() -> callback.onDataLoaded(tokens));
            } catch (Exception e) {
                postError(() -> callback.onError(e));
            }
        });
    }

    public void insertToken(Token token, DataOperationCallback callback) {
        executor.execute(() -> {
            try {
                token.normalize();
                tokenDao.insertToken(token);
                postSuccess(callback::onSuccess);
            } catch (Exception e) {
                postError(() -> callback.onError(e));
            }
        });
    }

    public void deleteToken(Token token, DataOperationCallback callback) {
        executor.execute(() -> {
            try {
                tokenDao.deleteToken(token);
                postSuccess(callback::onSuccess);
            } catch (Exception e) {
                postError(() -> callback.onError(e));
            }
        });
    }

    public void exportTokens(DataLoadCallback<String> callback) {
        executor.execute(() -> {
            try {
                List<Token> tokens = tokenDao.getAllTokens();
                String exportData = ImportExportUtils.exportToJson(tokens);
                postSuccess(() -> callback.onDataLoaded(exportData));
            } catch (Exception e) {
                postError(() -> callback.onError(e));
            }
        });
    }

    public void importTokens(String jsonData, DataOperationCallback callback) {
        executor.execute(() -> {
            try {
                List<Token> tokens = ImportExportUtils.importFromJson(jsonData);
                insertAll(tokens);
                postSuccess(callback::onSuccess);
            } catch (Exception e) {
                postError(() -> callback.onError(e));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void insertAll(List<Token> tokens) {
        List<Token> safeList = tokens == null ? new ArrayList<>() : tokens;
        for (Token token : safeList) {
            if (token == null) {
                continue;
            }
            token.setId(UUID.randomUUID().toString());
            token.normalize();
            tokenDao.insertToken(token);
        }
    }

    private void postSuccess(Runnable runnable) {
        mainHandler.post(runnable);
    }

    private void postError(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public interface DataLoadCallback<T> {
        void onDataLoaded(T data);

        void onError(Exception e);
    }

    public interface DataOperationCallback {
        void onSuccess();

        void onError(Exception e);
    }
}
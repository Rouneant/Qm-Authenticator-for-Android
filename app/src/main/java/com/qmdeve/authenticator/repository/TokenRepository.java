package com.qmdeve.authenticator.repository;

import android.content.Context;

import com.qmdeve.authenticator.data.AppDatabase;
import com.qmdeve.authenticator.data.dao.TokenDao;
import com.qmdeve.authenticator.model.Token;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenRepository {

    private final TokenDao tokenDao;
    private final ExecutorService executor;

    public TokenRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        tokenDao = database.tokenDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void getAllTokens(DataLoadCallback<List<Token>> callback) {
        executor.execute(() -> {
            try {
                List<Token> tokens = tokenDao.getAllTokens();
                callback.onDataLoaded(tokens);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void insertToken(Token token, DataOperationCallback callback) {
        executor.execute(() -> {
            try {
                tokenDao.insertToken(token);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteToken(Token token, DataOperationCallback callback) {
        executor.execute(() -> {
            try {
                tokenDao.deleteToken(token);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
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
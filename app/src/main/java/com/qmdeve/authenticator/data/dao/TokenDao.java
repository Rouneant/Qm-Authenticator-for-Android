package com.qmdeve.authenticator.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.qmdeve.authenticator.model.Token;

import java.util.List;

@Dao
public interface TokenDao {

    @Query("SELECT * FROM tokens ORDER BY id DESC")
    List<Token> getAllTokens();

    @Insert
    void insertToken(Token token);

    @Update
    void updateToken(Token token);

    @Delete
    void deleteToken(Token token);
}
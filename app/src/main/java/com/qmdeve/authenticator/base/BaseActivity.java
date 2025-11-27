package com.qmdeve.authenticator.base;

import android.os.Bundle;
import android.content.pm.ActivityInfo;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.qmdeve.authenticator.util.Utils;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        EdgeToEdge.enable(this);
        Utils.applyImmersiveSystemBars(getWindow());
    }
}
package com.qmdeve.authenticator.ui.activity;

import android.os.Bundle;
import android.view.ViewGroup;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.base.BaseActivity;
import com.qmdeve.authenticator.util.Utils;

public class HomeActivity extends BaseActivity {

    private MaterialToolbar toolbar;
    private FloatingActionButton fab;
    private int statusBarHeight = 0;
    private int navigationBarHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);

        fab.post(() -> {
            statusBarHeight = Utils.getStatusBarHeight(this);
            navigationBarHeight = Utils.getNavigationBarHeight(findViewById(android.R.id.content));

            setView();
        });
    }

    private void setView() {
        ViewGroup.MarginLayoutParams toolBarParams = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        toolBarParams.topMargin = statusBarHeight;
        toolbar.setLayoutParams(toolBarParams);

        ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        fabParams.bottomMargin = navigationBarHeight + Utils.dp2px(this, 24);
        fabParams.rightMargin = Utils.dp2px(this, 24);
        fab.setLayoutParams(fabParams);
    }
}
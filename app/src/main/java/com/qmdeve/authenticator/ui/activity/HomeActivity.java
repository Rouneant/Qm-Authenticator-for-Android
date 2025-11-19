package com.qmdeve.authenticator.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.adapter.TokenAdapter;
import com.qmdeve.authenticator.base.BaseActivity;
import com.qmdeve.authenticator.model.Token;
import com.qmdeve.authenticator.repository.TokenRepository;
import com.qmdeve.authenticator.ui.dialog.AddTokenDialog;
import com.qmdeve.authenticator.util.TOTPGenerator;
import com.qmdeve.authenticator.util.Utils;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private MaterialToolbar toolbar;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private TokenAdapter tokenAdapter;
    private TokenRepository tokenRepository;
    private int statusBarHeight = 0;
    private int navigationBarHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();
        setRecyclerView();
        setClickListeners();
        loadTokens();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recycler_view);
        tokenRepository = new TokenRepository(this);

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

    private void setRecyclerView() {
        tokenAdapter = new TokenAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(tokenAdapter);

        setTokenAdapterListeners();
    }

    private void setTokenAdapterListeners() {
        tokenAdapter.setOnTokenClickListener(new TokenAdapter.OnTokenClickListener() {
            @Override
            public void onTokenClick(Token token) {
                copyTokenToClipboard(token);
            }

            @Override
            public void onTokenLongClick(Token token) {
                showTokenOptionsDialog(token);
            }
        });
    }

    private void setClickListeners() {
        fab.setOnClickListener(v -> showAddTokenDialog());
    }

    private void showAddTokenDialog() {
        AddTokenDialog dialog = new AddTokenDialog();
        dialog.setOnTokenAddedListener(token -> addTokenToDatabase(token));
        dialog.show(getSupportFragmentManager(), "AddTokenDialog");
    }

    private void addTokenToDatabase(Token token) {
        tokenRepository.insertToken(token, new TokenRepository.DataOperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    loadTokens();
                    showSnackbar("令牌添加成功");
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showSnackbar("添加失败: " + e.getMessage());
                });
            }
        });
    }

    private void loadTokens() {
        tokenRepository.getAllTokens(new TokenRepository.DataLoadCallback<>() {
            @Override
            public void onDataLoaded(List<Token> tokens) {
                runOnUiThread(() -> {
                    tokenAdapter.setTokens(tokens);
                    if (tokens.isEmpty()) {
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    showSnackbar("加载失败: " + e.getMessage());
                });
            }
        });
    }

    private void copyTokenToClipboard(Token token) {
        try {
            String code = TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("验证码", code);
            clipboard.setPrimaryClip(clip);

            showSnackbar("验证码已复制到剪贴板");
        } catch (Exception e) {
            showSnackbar("复制失败: " + e.getMessage());
        }
    }

    private void showTokenOptionsDialog(Token token) {
        String[] options = new String[]{"复制验证码", "查看详情", "删除令牌"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(token.getLabel())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            copyTokenToClipboard(token);
                            break;
                        case 1:
                            showTokenDetails(token);
                            break;
                        case 2:
                            showDeleteTokenDialog(token);
                            break;
                    }
                })
                .setNegativeButton(getText(R.string.cancel), null)
                .show();
    }

    private void showTokenDetails(Token token) {
        String code;
        try {
            code = TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());
        } catch (Exception e) {
            code = "Error";
        }

        String details = getText(R.string.type) + ": TOTP" + "\n" +
                getText(R.string.issuer) + ": " + (token.getIssuer() != null ? token.getIssuer() : getText(R.string.not_set)) + "\n" +
                getText(R.string.account_hint) + ": " + token.getAccount() + "\n" +
                getText(R.string.verification_code) + ": " + code;

        new MaterialAlertDialogBuilder(this)
                .setTitle("令牌详情")
                .setMessage(details)
                .setPositiveButton("确定", null)
                .setNeutralButton("复制密钥", (dialog, which) -> {
                    copySecretToClipboard(token);
                })
                .show();
    }

    private void copySecretToClipboard(Token token) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("密钥", token.getSecret());
        clipboard.setPrimaryClip(clip);
        showSnackbar("密钥已复制到剪贴板");
    }

    private void showDeleteTokenDialog(Token token) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除令牌")
                .setMessage("确定要删除 " + token.getLabel() + " 吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteToken(token))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteToken(Token token) {
        tokenRepository.deleteToken(token, new TokenRepository.DataOperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    tokenAdapter.removeToken(token);
                    showSnackbar("令牌已删除");

                    if (tokenAdapter.getItemCount() == 0) {
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> showSnackbar("删除失败: " + e.getMessage()));
            }
        });
    }

    private void showEmptyState() {
        showSnackbar("暂无令牌，点击右下角按钮添加");
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenAdapter != null) {
            tokenAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tokenAdapter != null) {
            tokenAdapter.onDetachedFromRecyclerView(recyclerView);
        }
    }
}
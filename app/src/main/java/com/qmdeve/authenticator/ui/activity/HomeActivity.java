package com.qmdeve.authenticator.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.WriterException;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.adapter.TokenAdapter;
import com.qmdeve.authenticator.base.BaseActivity;
import com.qmdeve.authenticator.model.Token;
import com.qmdeve.authenticator.repository.TokenRepository;
import com.qmdeve.authenticator.ui.dialog.AddTokenDialog;
import com.qmdeve.authenticator.util.GitHubUpdateChecker;
import com.qmdeve.authenticator.util.ImportExportUtils;
import com.qmdeve.authenticator.util.QrCodeGenerator;
import com.qmdeve.authenticator.util.TOTPGenerator;
import com.qmdeve.authenticator.util.TOTPUriParser;
import com.qmdeve.authenticator.util.Utils;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private static final int EXPORT_QR_SIZE = 1024;
    private MaterialToolbar toolbar;
    private FloatingActionButton fab;
    private RecyclerView tokenRecyclerView;
    private TokenAdapter tokenAdapter;
    private TokenRepository tokenRepository;
    private GitHubUpdateChecker updateChecker;
    private Snackbar currentSnackbar;

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleScanActivityResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenRepository = new TokenRepository(this);
        updateChecker = new GitHubUpdateChecker(this);

        initViews();
        initToolbar();
        initRecyclerView();
        initFab();

        loadTokens();
        checkForUpdates();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        tokenRecyclerView = findViewById(R.id.recycler_view);

        fab.post(this::adjustInsets);
    }

    private void adjustInsets() {
        int statusBarHeight = Utils.getStatusBarHeight(this);
        int navigationBarHeight = Utils.getNavigationBarHeight(findViewById(android.R.id.content));

        ViewGroup.MarginLayoutParams toolbarParams = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        toolbarParams.topMargin = statusBarHeight;
        toolbar.setLayoutParams(toolbarParams);

        ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        fabParams.bottomMargin = navigationBarHeight + Utils.dp2px(this, 24);
        fabParams.rightMargin = Utils.dp2px(this, 24);
        fab.setLayoutParams(fabParams);
    }

    private void initToolbar() {
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_export) {
                exportTokens();
                return true;
            } else if (id == R.id.menu_github) {
                Utils.openExternalLink(this, "https://github.com/Rouneant/Qm-Authenticator-for-Android");
                return true;
            } else if (id == R.id.menu_telegram) {
                Utils.openExternalLink(this, "https://t.me/QmDeves");
                return true;
            } else if (id == R.id.menu_qq) {
                Utils.openExternalLink(this, "https://qm.qq.com/q/OEVn8ZslMq");
                return true;
            }
            return false;
        });
    }

    private void initRecyclerView() {
        tokenAdapter = new TokenAdapter();
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

        tokenRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tokenRecyclerView.setAdapter(tokenAdapter);
    }

    private void initFab() {
        fab.setOnClickListener(v -> showAddOptions());
    }

    private void showAddOptions() {
        final String[] items = {getString(R.string.enter_setting_key), getString(R.string.scan_qr_code), getString(R.string.import_qm)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.adding_method))
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAddTokenDialog();
                            break;
                        case 1:
                        case 2:
                            launchScanner();
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void launchScanner() {
        scanLauncher.launch(new Intent(this, ScanActivity.class));
    }

    private void showAddTokenDialog() {
        AddTokenDialog dialog = new AddTokenDialog();
        dialog.setOnTokenAddedListener(this::addTokenToDatabase);
        dialog.show(getSupportFragmentManager(), "AddKeyDialog");
    }

    private void addTokenToDatabase(Token token) {
        tokenRepository.insertToken(token, new TokenRepository.DataOperationCallback() {
            @Override
            public void onSuccess() {
                loadTokens();
                showSnackbar(getString(R.string.successfully));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage(e);
            }
        });
    }

    private void loadTokens() {
        tokenRepository.getAllTokens(new TokenRepository.DataLoadCallback<>() {
            @Override
            public void onDataLoaded(List<Token> tokens) {
                tokenAdapter.setTokens(tokens);
                if (tokens.isEmpty()) {
                    showSnackbar(getString(R.string.no_tokens_message));
                }
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage(e);
            }
        });
    }

    private void handleScanActivityResult(ActivityResult activityResult) {
        if (activityResult.getResultCode() == ScanActivity.RESULT_SCAN_SUCCESS && activityResult.getData() != null) {
            String scanResult = activityResult.getData().getStringExtra(ScanActivity.EXTRA_SCAN_RESULT);
            if (scanResult != null) {
                handleScanPayload(scanResult);
            }
        } else if (activityResult.getResultCode() == ScanActivity.RESULT_SCAN_FAILED) {
            showSnackbar(getString(R.string.scan_failed));
        }
    }

    private void handleScanPayload(String payload) {
        if (ImportExportUtils.validateExportData(payload)) {
            tokenRepository.importTokens(payload, new TokenRepository.DataOperationCallback() {
                @Override
                public void onSuccess() {
                    loadTokens();
                    showSnackbar(getString(R.string.successfully));
                }

                @Override
                public void onError(Exception e) {
                    showErrorMessage(e);
                }
            });
            return;
        }
        try {
            Token token = TOTPUriParser.parseTotpUri(payload);
            addTokenToDatabase(token);
        } catch (IllegalArgumentException e) {
            showSnackbar(getString(R.string.invalid_qr_code));
        } catch (Exception e) {
            showSnackbar(getString(R.string.error_occurred_while_processing_qr_code));
        }
    }

    private void copyTokenToClipboard(Token token) {
        String code = TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());
        if (code == null) {
            showSnackbar(getString(R.string.failed_generate_verification_code));
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.verification_code), code);
        clipboard.setPrimaryClip(clip);
        showSnackbar(getString(R.string.copy_successfully));
    }

    private void showTokenOptionsDialog(Token token) {
        String[] options = {
                getString(R.string.copy_verification_code),
                getString(R.string.view_details),
                getString(R.string.delete_token)
        };

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
                            confirmDelete(token);
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showTokenDetails(Token token) {
        String code = TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());
        if (code == null) {
            code = getString(R.string.code_error_placeholder);
        }
        String details = getString(R.string.type) + ": TOTP\n"
                + getString(R.string.issuer) + ": " + (token.getIssuer() != null ? token.getIssuer() : getString(R.string.not_set)) + "\n"
                + getString(R.string.account_hint) + ": " + token.getAccount() + "\n"
                + getString(R.string.verification_code) + ": " + code;

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.token_details))
                .setMessage(details)
                .setPositiveButton(getString(R.string.confirm), null)
                .setNeutralButton(getString(R.string.copy_key), (dialog, which) -> copySecretToClipboard(token))
                .show();
    }

    private void copySecretToClipboard(Token token) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.key), token.getSecret());
        clipboard.setPrimaryClip(clip);
        showSnackbar(getString(R.string.copy_key_successfully));
    }

    private void confirmDelete(Token token) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_token_title)
                .setMessage(getString(R.string.delete_token_message, token.getLabel()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteToken(token))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteToken(Token token) {
        tokenRepository.deleteToken(token, new TokenRepository.DataOperationCallback() {
            @Override
            public void onSuccess() {
                tokenAdapter.removeToken(token);
                showSnackbar(getString(R.string.token_deleted));
                if (tokenAdapter.getItemCount() == 0) {
                    showSnackbar(getString(R.string.no_tokens_message));
                }
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage(e);
            }
        });
    }

    private void exportTokens() {
        tokenRepository.exportTokens(new TokenRepository.DataLoadCallback<>() {
            @Override
            public void onDataLoaded(String exportData) {
                try {
                    Bitmap qrCode = QrCodeGenerator.generateQrCode(exportData, EXPORT_QR_SIZE, EXPORT_QR_SIZE);
                    showQrCodeDialog(qrCode);
                } catch (WriterException e) {
                    showSnackbar(getString(R.string.failed_generate_qr));
                }
            }

            @Override
            public void onError(Exception e) {
                showSnackbar(getString(R.string.export_failed));
            }
        });
    }

    private void showQrCodeDialog(Bitmap qrCode) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);
        ImageView qrCodeImageView = dialogView.findViewById(R.id.qr_code_image);
        qrCodeImageView.setImageBitmap(qrCode);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.cancel), null)
                .show();
    }

    private void checkForUpdates() {
        updateChecker.checkForUpdates(new GitHubUpdateChecker.UpdateCheckListener() {
            @Override
            public void onUpdateAvailable(String latestVersion, String downloadUrl, String releaseBody) {
                Utils.showUpdateDialog(HomeActivity.this, latestVersion, downloadUrl, releaseBody);
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void showSnackbar(String message) {
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
        }
        currentSnackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        currentSnackbar.setAnchorView(fab);
        currentSnackbar.show();
    }

    private void showErrorMessage(Exception e) {
        showSnackbar("Error: " + e.getMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tokenRepository != null) {
            tokenRepository.shutdown();
        }
        if (updateChecker != null) {
            updateChecker.destroy();
        }
    }
}

package com.qmdeve.authenticator.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.model.Token;

import java.util.Objects;

public class AddTokenDialog extends DialogFragment {

    private TextInputEditText issuerEditText;
    private TextInputEditText accountEditText;
    private TextInputEditText secretEditText;
    private TextInputLayout accountInputLayout;
    private TextInputLayout secretInputLayout;
    private MaterialButton cancelButton;
    private MaterialButton confirmButton;

    private OnTokenAddedListener listener;

    public interface OnTokenAddedListener {
        void onTokenAdded(Token token);
    }

    public void setOnTokenAddedListener(OnTokenAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_verify_key))
                .setView(createDialogView())
                .create();
    }

    private View createDialogView() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_token, null);
        initView(view);
        setupClickListeners();
        return view;
    }

    private void initView(View view) {
        accountInputLayout = view.findViewById(R.id.account_input_layout);
        secretInputLayout = view.findViewById(R.id.secret_input_layout);
        issuerEditText = view.findViewById(R.id.issuer_edit_text);
        accountEditText = view.findViewById(R.id.account_edit_text);
        secretEditText = view.findViewById(R.id.secret_edit_text);
        cancelButton = view.findViewById(R.id.cancel_button);
        confirmButton = view.findViewById(R.id.confirm_button);
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> dismiss());
        confirmButton.setOnClickListener(v -> {
            if (validateInput()) {
                addToken();
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;
        clearErrors();

        if (TextUtils.isEmpty(getText(accountEditText))) {
            accountInputLayout.setError(getString(R.string.enter_account_name));
            isValid = false;
        }

        String secret = normalizeSecret(getText(secretEditText));
        if (TextUtils.isEmpty(secret)) {
            secretInputLayout.setError(getString(R.string.enter_secret_key));
            isValid = false;
        }

        return isValid;
    }

    private void addToken() {
        String issuer = getText(issuerEditText);
        String account = getText(accountEditText);
        String secret = normalizeSecret(getText(secretEditText));

        Token token = new Token(issuer, account, secret, Token.TokenType.TOTP);
        if (listener != null) {
            listener.onTokenAdded(token);
        }
        dismiss();
    }

    private void clearErrors() {
        accountInputLayout.setError(null);
        secretInputLayout.setError(null);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String normalizeSecret(String secret) {
        return secret == null ? "" : secret.replaceAll("\\s+", "").toUpperCase();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
package com.qmdeve.authenticator.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.model.Token;

public class AddTokenDialog extends androidx.fragment.app.DialogFragment {

    private TextInputEditText issuerEditText;
    private TextInputEditText accountEditText;
    private TextInputEditText secretEditText;
    private TextInputLayout issuerInputLayout;
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("添加验证令牌");
        builder.setView(createDialogView());
        return builder.create();
    }

    private View createDialogView() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_token, null);

        initView(view);
        setupClickListeners();

        return view;
    }

    private void initView(View view) {
        issuerInputLayout = view.findViewById(R.id.issuer_input_layout);
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

        if (TextUtils.isEmpty(accountEditText.getText())) {
            accountInputLayout.setError("请输入账户名");
            isValid = false;
        } else {
            accountInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(secretEditText.getText())) {
            secretInputLayout.setError("请输入密钥");
            isValid = false;
        } else {
            secretInputLayout.setError(null);
        }

        return isValid;
    }

    private void addToken() {
        String issuer = issuerEditText.getText() != null ?
                issuerEditText.getText().toString().trim() : "";
        String account = accountEditText.getText() != null ?
                accountEditText.getText().toString().trim() : "";
        String secret = secretEditText.getText() != null ?
                secretEditText.getText().toString().trim().toUpperCase() : "";
        secret = secret.replaceAll("\\s+", "");

        Token token = new Token(issuer, account, secret, Token.TokenType.TOTP);

        if (listener != null) {
            listener.onTokenAdded(token);
        }

        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}
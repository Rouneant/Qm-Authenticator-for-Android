package com.qmdeve.authenticator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.model.Token;
import com.qmdeve.authenticator.util.TOTPGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    private List<Token> tokens = new ArrayList<>();
    private Timer timer;
    private RecyclerView recyclerView;
    private OnTokenClickListener listener;

    public interface OnTokenClickListener {
        void onTokenClick(Token token);

        void onTokenLongClick(Token token);
    }

    public void setOnTokenClickListener(OnTokenClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new TokenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        Token token = tokens.get(position);
        holder.bind(token);
    }

    @Override
    public int getItemCount() {
        return tokens.size();
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens != null ? tokens : new ArrayList<>();
        notifyDataSetChanged();
        startTimer();
    }

    public void removeToken(Token token) {
        if (token != null) {
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).getId().equals(token.getId())) {
                    tokens.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        if (tokens.isEmpty()) {
            return;
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (recyclerView != null && recyclerView.getAdapter() != null) {
                    recyclerView.post(() -> updateTimersOnly());
                }
            }
        }, 0, 1000);
    }

    private void updateTimersOnly() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
            if (viewHolder instanceof TokenViewHolder) {
                ((TokenViewHolder) viewHolder).updateCodeAndTime(token);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        this.recyclerView = null;
    }

    class TokenViewHolder extends RecyclerView.ViewHolder {
        private TextView issuerText;
        private TextView accountText;
        private TextView codeText;
        private CircularProgressIndicator progressIndicator;
        private Token currentToken;

        public TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            issuerText = itemView.findViewById(R.id.issuer_text);
            accountText = itemView.findViewById(R.id.account_text);
            codeText = itemView.findViewById(R.id.code_text);
            progressIndicator = itemView.findViewById(R.id.progress_indicator);

            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setClickable(true);
            itemView.setFocusable(true);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Token token = tokens.get(position);
                    listener.onTokenClick(token);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Token token = tokens.get(position);
                    listener.onTokenLongClick(token);
                    return true;
                }
                return false;
            });
        }

        public void bind(Token token) {
            this.currentToken = token;

            if (token.getIssuer() != null && !token.getIssuer().trim().isEmpty()) {
                issuerText.setText(token.getIssuer());
                issuerText.setVisibility(TextView.VISIBLE);
            } else {
                issuerText.setVisibility(TextView.GONE);
            }

            accountText.setText(token.getAccount());

            updateCodeAndTime(token);
        }

        public void updateCodeAndTime(Token token) {
            if (!token.getId().equals(currentToken.getId())) {
                return;
            }

            String code = generateCode(token);
            codeText.setText(formatCode(code));

            long currentTime = System.currentTimeMillis() / 1000;
            long remainingTime = token.getPeriod() - (currentTime % token.getPeriod());

            progressIndicator.setMax(token.getPeriod());
            progressIndicator.setProgress((int) remainingTime);
            progressIndicator.setVisibility(View.VISIBLE);

            if (remainingTime <= 10) {
                codeText.setTextColor(itemView.getContext().getColor(R.color.material_red_500));
                progressIndicator.setIndicatorColor(itemView.getContext().getColor(R.color.material_red_500));
            } else {
                codeText.setTextColor(itemView.getContext().getColor(R.color.material_on_surface_variant));
                progressIndicator.setIndicatorColor(itemView.getContext().getColor(R.color.material_on_surface_variant));
            }
        }

        private String generateCode(Token token) {
            try {
                return TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }

        private String formatCode(String code) {
            if (code == null || code.length() != 6) {
                return code;
            }
            return code.substring(0, 3) + " " + code.substring(3);
        }
    }
}
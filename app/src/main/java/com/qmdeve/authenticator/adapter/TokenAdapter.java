package com.qmdeve.authenticator.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.qmdeve.authenticator.R;
import com.qmdeve.authenticator.model.Token;
import com.qmdeve.authenticator.util.TOTPGenerator;

import java.util.ArrayList;
import java.util.List;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    private static final Object PAYLOAD_TIMER = new Object();
    private final List<Token> tokens = new ArrayList<>();
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!tokens.isEmpty()) {
                notifyItemRangeChanged(0, tokens.size(), PAYLOAD_TIMER);
                tickerHandler.postDelayed(this, 1000L);
            }
        }
    };

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
        holder.bind(tokens.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_TIMER)) {
            holder.updateCodeAndTime(tokens.get(position));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return tokens.size();
    }

    public void setTokens(List<Token> newTokens) {
        List<Token> safeTokens = newTokens == null ? new ArrayList<>() : new ArrayList<>(newTokens);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TokenDiffCallback(tokens, safeTokens));
        tokens.clear();
        tokens.addAll(safeTokens);
        diffResult.dispatchUpdatesTo(this);
        restartTicker();
    }

    public void removeToken(Token token) {
        if (token == null) {
            return;
        }
        List<Token> updated = new ArrayList<>(tokens);
        for (Token existing : tokens) {
            if (existing.getId().equals(token.getId())) {
                updated.remove(existing);
                    break;
                }
            }
        setTokens(updated);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        restartTicker();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopTicker();
    }

    private void restartTicker() {
        stopTicker();
        if (!tokens.isEmpty()) {
            tickerHandler.post(tickerRunnable);
        }
    }

    private void stopTicker() {
        tickerHandler.removeCallbacksAndMessages(null);
    }

    class TokenViewHolder extends RecyclerView.ViewHolder {
        private final TextView issuerText;
        private final TextView accountText;
        private final TextView codeText;
        private final CircularProgressIndicator progressIndicator;
        private final Context context;
        private final int normalColor;
        private final int dangerColor;
        private Token currentToken;

        TokenViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            issuerText = itemView.findViewById(R.id.issuer_text);
            accountText = itemView.findViewById(R.id.account_text);
            codeText = itemView.findViewById(R.id.code_text);
            progressIndicator = itemView.findViewById(R.id.progress_indicator);
            normalColor = progressIndicator.getIndicatorColor()[0];
            dangerColor = context.getColor(R.color.material_red_500);
            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTokenClick(tokens.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTokenLongClick(tokens.get(position));
                    return true;
                }
                return false;
            });
        }

        void bind(Token token) {
            currentToken = token;
            if (token.getIssuer() != null && !token.getIssuer().isEmpty()) {
                issuerText.setText(token.getIssuer());
                issuerText.setVisibility(View.VISIBLE);
            } else {
                issuerText.setVisibility(View.GONE);
            }
            accountText.setText(token.getAccount());
            updateCodeAndTime(token);
        }

        void updateCodeAndTime(Token token) {
            if (currentToken == null || !currentToken.getId().equals(token.getId())) {
                return;
            }
            String code = TOTPGenerator.generateTOTP(token.getSecret(), token.getPeriod(), token.getDigits());
            codeText.setText(formatCode(code));
            updateProgress(token);
        }

        private void updateProgress(Token token) {
            long currentTime = System.currentTimeMillis() / 1000;
            int period = Math.max(token.getPeriod(), Token.DEFAULT_PERIOD);
            int remainingTime = (int) (period - (currentTime % period));

            progressIndicator.setMax(period);
            progressIndicator.setProgress(remainingTime, true);

            if (remainingTime <= 10) {
                codeText.setTextColor(dangerColor);
                progressIndicator.setIndicatorColor(dangerColor);
            } else {
                codeText.setTextColor(normalColor);
                progressIndicator.setIndicatorColor(normalColor);
            }
        }

        private String formatCode(String code) {
            if (code == null || code.length() <= 3) {
                return context.getString(R.string.code_error_placeholder);
            }
            int midpoint = code.length() / 2;
            return code.substring(0, midpoint) + " " + code.substring(midpoint);
        }
    }

    private static class TokenDiffCallback extends DiffUtil.Callback {

        private final List<Token> oldList;
        private final List<Token> newList;

        TokenDiffCallback(List<Token> oldList, List<Token> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Token oldToken = oldList.get(oldItemPosition);
            Token newToken = newList.get(newItemPosition);
            return oldToken.getAccount().equals(newToken.getAccount())
                    && ((oldToken.getIssuer() == null && newToken.getIssuer() == null)
                    || (oldToken.getIssuer() != null && oldToken.getIssuer().equals(newToken.getIssuer())))
                    && oldToken.getSecret().equals(newToken.getSecret())
                    && oldToken.getDigits() == newToken.getDigits()
                    && oldToken.getPeriod() == newToken.getPeriod();
        }
    }
}
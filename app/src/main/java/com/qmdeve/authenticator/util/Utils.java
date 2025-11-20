package com.qmdeve.authenticator.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import io.noties.markwon.Markwon;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qmdeve.authenticator.R;

public class Utils {
    public static void immersiveNavigationBar(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
        systemUiVisibility = systemUiVisibility | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public static void immersiveStatusBar(Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
        systemUiVisibility = systemUiVisibility | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    public static int getNavigationBarHeight(View view) {
        WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(view);
        if (rootWindowInsets != null) {
            Insets navigationBars = rootWindowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            return navigationBars.bottom;
        }
        return 0;
    }

    public static int getStatusBarHeight(Context context) {
        @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
        int resId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android"
        );
        return context.getResources().getDimensionPixelSize(resId);
    }

    public static int dp2px(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static void showUpdateDialog(Context context, String version, String url, String updateContent) {
        final Markwon markwon = Markwon.builder(context).build();
        TextView textView = new TextView(context);
        textView.setPadding(50, 40, 50, 40);
        markwon.setMarkdown(textView, updateContent);

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.new_version) + " (" + version + ")")
                .setView(textView)
                .setPositiveButton("Update", (dialog, which) -> {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                })
                .setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> {})
                .show();
    }
}
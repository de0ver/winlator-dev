package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.widget.LogView;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DebugDialog extends ContentDialog implements Callback<String> {
    private final LogView logView;
    private static boolean paused = false;
    private final BufferedWriter writer;
    private final Activity activity;
    private static final Set<String> fatal_errors = new HashSet<>(Arrays.asList(
            " fault",
            "explicit kill",
            "destroyed from the outside",
            "err:module:import_dll Loading library",
            "Unhandled page fault",
            "err:msvcrt:_wassert"
    ));

    public DebugDialog(@NonNull Activity context) {
        super(context, R.layout.debug_dialog);
        setIcon(R.drawable.icon_debug);
        setTitle(context.getString(R.string.logs));

        activity = context;

        logView = findViewById(R.id.LogView);
        
        logView.getLayoutParams().width = (int)UnitUtils.dpToPx(UnitUtils.pxToDp(AppUtils.getScreenWidth()) * 0.7f);

        findViewById(R.id.BTCancel).setVisibility(View.GONE);

        LinearLayout llBottomBarPanel = findViewById(R.id.LLBottomBarPanel);
        llBottomBarPanel.setVisibility(View.VISIBLE);

        View toolbarView = LayoutInflater.from(context).inflate(R.layout.debug_toolbar, llBottomBarPanel, false);
        toolbarView.findViewById(R.id.BTClear).setOnClickListener((v) -> logView.clear());
        toolbarView.findViewById(R.id.BTPause).setOnClickListener((v) -> {
            setPaused(!paused);
            ((ImageButton)v).setImageResource(getPaused() ? R.drawable.icon_play : R.drawable.icon_pause);
        });
        llBottomBarPanel.addView(toolbarView);
        try {
            writer = new BufferedWriter(new FileWriter(LogView.getLogFile()));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void call(final String line) {
        if (!getPaused()) {
            logView.append(line + "\n");
        }
        try {
            if (isFatalError(line))
                AppUtils.showToastError(activity, "Fatal Error:  \n" + line);
            writer.write(line + "\n"); //ehehhe 1 space before [time]
            writer.flush();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isFatalError(String line) {
        for (String keyword : fatal_errors) {
            if (line.contains(keyword)) return true;
        }
        return false;
    }
    
    public static void setPaused(boolean cond) {
        paused = cond;
    }
    
    public static boolean getPaused() {
        return paused;
    }
}

package com.winlator.contentdialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;

import java.util.ArrayList;
import java.util.List;

import app.gamenative.R;

/**
 * Debug dialog that lists all mapped X11 windows and their current focus state.
 * Tap any row to force keyboard focus onto that window.
 *
 * Useful for diagnosing input issues where the game window is not receiving
 * keyboard/mouse events.
 */
public class WindowFocusDebugDialog extends Dialog {

    private static final int COLOR_BG        = 0xFF1E1E2E;
    private static final int COLOR_ROW_FOCUS = 0xFF2D4A2D;
    private static final int COLOR_DIVIDER   = 0xFF333350;
    private static final int COLOR_MUTED     = 0xFFAAAAAA;
    private static final int COLOR_GREEN     = 0xFF88FF88;

    public WindowFocusDebugDialog(@NonNull Context context, @NonNull XServer xServer) {
        super(context, R.style.ContentDialog);

        List<WindowEntry> entries = new ArrayList<>();
        int focusedId;
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            Window focused = xServer.windowManager.getFocusedWindow();
            focusedId = focused != null ? focused.id : -1;
            collectWindows(xServer.windowManager.rootWindow, entries, focusedId);
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(context);
        title.setText("X11 Window Focus");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(2));
        root.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("Tap a window to force keyboard focus onto it");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 0, 0, dp(12));
        root.addView(subtitle);

        ScrollView scrollView = new ScrollView(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (dm.heightPixels * 0.65f)));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list);

        if (entries.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("No mapped windows found");
            empty.setTextColor(COLOR_MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            list.addView(empty);
        } else {
            for (WindowEntry entry : entries) {
                list.addView(buildRow(context, xServer, entry));
            }
        }

        root.addView(scrollView);
        setContentView(root);
    }

    private View buildRow(Context context, XServer xServer, WindowEntry entry) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        int padV = dp(10);
        int padH = dp(8);
        row.setPadding(padH, padV, padH, padV);
        if (entry.isFocused) row.setBackgroundColor(COLOR_ROW_FOCUS);

        // ── Name line + FOCUSED badge ─────────────────────────────────────
        LinearLayout nameLine = new LinearLayout(context);
        nameLine.setOrientation(LinearLayout.HORIZONTAL);

        TextView nameView = new TextView(context);
        nameView.setText(entry.displayName);
        nameView.setTextColor(entry.isFocused ? COLOR_GREEN : Color.WHITE);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextSize(14);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        nameLine.addView(nameView);

        if (entry.isFocused) {
            TextView badge = new TextView(context);
            badge.setText("● FOCUSED");
            badge.setTextColor(COLOR_GREEN);
            badge.setTextSize(11);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER_VERTICAL);
            nameLine.addView(badge);
        }
        row.addView(nameLine);

        // ── Details line ─────────────────────────────────────────────────
        TextView details = new TextView(context);
        details.setText(entry.details);
        details.setTextColor(COLOR_MUTED);
        details.setTextSize(11);
        details.setPadding(0, dp(2), 0, 0);
        row.addView(details);

        // ── Divider ───────────────────────────────────────────────────────
        View divider = new View(context);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divLp.topMargin = dp(8);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(COLOR_DIVIDER);
        row.addView(divider);

        row.setOnClickListener(v -> {
            try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                Window target = xServer.windowManager.getWindow(entry.windowId);
                if (target != null) {
                    xServer.windowManager.setFocus(target, WindowManager.FocusRevertTo.POINTER_ROOT);
                }
            }
            Toast.makeText(context, "Focus → " + entry.displayName, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return row;
    }

    // ── Window collection ─────────────────────────────────────────────────────

    private void collectWindows(Window parent, List<WindowEntry> entries, int focusedId) {
        for (Window child : parent.getChildren()) {
            if (child.attributes.isMapped() && child.isInputOutput()) {
                int w = child.getWidth() & 0xFFFF;
                int h = child.getHeight() & 0xFFFF;
                String name = child.getName();
                String cls  = child.getClassName();
                int pid     = child.getProcessId();

                boolean hasName = name != null && !name.isEmpty();
                // Skip invisible utility windows with no name (tooltips, menus <50px)
                boolean bigEnough = w > 50 && h > 50;

                if (hasName || bigEnough) {
                    String displayName = hasName ? name : "Window #" + child.id;

                    // WM_CLASS is two null-separated strings: "instance\0class\0"
                    String cleanCls = "";
                    if (cls != null && !cls.isEmpty()) {
                        cleanCls = cls.replace("\0", " / ").trim();
                        // strip a trailing " / " if present
                        if (cleanCls.endsWith(" /")) cleanCls = cleanCls.substring(0, cleanCls.length() - 2).trim();
                    }

                    StringBuilder det = new StringBuilder();
                    det.append(w).append("×").append(h);
                    if (!cleanCls.isEmpty()) det.append("  cls: ").append(cleanCls);
                    if (pid > 0)            det.append("  pid: ").append(pid);
                    det.append("  xid: 0x").append(Integer.toHexString(child.id));

                    boolean focused = child.id == focusedId;
                    entries.add(new WindowEntry(child.id, displayName, det.toString(), focused));
                }
            }
            // Recurse regardless of mapped/visible state — children of an unmapped
            // parent can still be interesting (Wine virtual desktop is one such case).
            collectWindows(child, entries, focusedId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int dp(float dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    private static final class WindowEntry {
        final int    windowId;
        final String displayName;
        final String details;
        final boolean isFocused;

        WindowEntry(int windowId, String displayName, String details, boolean isFocused) {
            this.windowId    = windowId;
            this.displayName = displayName;
            this.details     = details;
            this.isFocused   = isFocused;
        }
    }
}

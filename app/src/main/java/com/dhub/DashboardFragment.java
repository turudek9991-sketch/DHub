package com.dhub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView statusDot, statusText, statProfiles, statRestarts, statDetector;
    private LinearLayout profileListContainer;
    private Button btnStart, btnStop;
    private TextView logText, btnClearLog;
    private ScrollView logScroll;

    private AppPrefs prefs;
    private List<ProfileModel> profiles;
    private boolean isRunning = false;
    private int restartCount = 0;
    private SpannableStringBuilder logBuilder = new SpannableStringBuilder();

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MonitorService.ACTION_LOG.equals(intent.getAction())) {
                String msg = intent.getStringExtra(MonitorService.EXTRA_LOG_MSG);
                String type = intent.getStringExtra(MonitorService.EXTRA_LOG_TYPE);
                appendLog(msg, type);
            } else if (MonitorService.ACTION_RESTART_COUNT.equals(intent.getAction())) {
                restartCount = intent.getIntExtra(MonitorService.EXTRA_RESTART_COUNT, 0);
                updateStats();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new AppPrefs(requireContext());
        profiles = prefs.loadProfiles();

        statusDot = view.findViewById(R.id.status_dot);
        statusText = view.findViewById(R.id.status_text);
        statProfiles = view.findViewById(R.id.stat_profiles);
        statRestarts = view.findViewById(R.id.stat_restarts);
        statDetector = view.findViewById(R.id.stat_detector);
        profileListContainer = view.findViewById(R.id.profile_list_container);
        btnStart = view.findViewById(R.id.btn_start);
        btnStop = view.findViewById(R.id.btn_stop);
        logText = view.findViewById(R.id.log_text);
        logScroll = view.findViewById(R.id.log_scroll);
        btnClearLog = view.findViewById(R.id.btn_clear_log);

        updateStats();

        btnStart.setOnClickListener(v -> handleStart());
        btnStop.setOnClickListener(v -> handleStop());
        btnClearLog.setOnClickListener(v -> {
            logBuilder.clear();
            prefs.clearLogs();
            logText.setText("Belum ada aktivitas");
            logText.setTextColor(Color.parseColor("#4A4A70"));
            logText.setGravity(android.view.Gravity.CENTER);
            logText.setPadding(0, 80, 0, 0);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        profiles = prefs.loadProfiles();
        
        // Pemulihan Log dari Prefs
        String savedLogs = prefs.loadLogs();
        if (!savedLogs.isEmpty()) {
            logBuilder = new SpannableStringBuilder(Html.fromHtml(savedLogs, Html.FROM_HTML_MODE_LEGACY));
            logText.setText(logBuilder);
            logText.setPadding(0, 0, 0, 0);
            logText.setGravity(android.view.Gravity.START);
        }

        // Sinkronisasi status deteksi service yang sedang berjalan
        if (MonitorService.isServiceRunning) {
            isRunning = true;
            setStatus("RUNNING", "#4ADE80");
            showProfileList(true);
        } else {
            isRunning = false;
            setStatus("STOPPED", "#7070A0");
            showProfileList(false);
        }

        updateStats();
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(MonitorService.ACTION_LOG);
        filter.addAction(MonitorService.ACTION_RESTART_COUNT);
        requireContext().registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(logReceiver); } catch (Exception ignored) {}
    }

    private void handleStart() {
        profiles = prefs.loadProfiles();
        if (profiles.isEmpty()) {
            appendLog("⚠ Tidak ada profil! Tambahkan package di Settings.", "red");
            return;
        }

        isRunning = true;
        restartCount = 0;
        logBuilder.clear();
        prefs.clearLogs();
        logText.setPadding(0, 0, 0, 0);
        logText.setGravity(android.view.Gravity.START);

        setStatus("STARTING", "#FBBF24");
        appendLog("Detector terpasang (" + profiles.size() + " executor)", "accent");
        appendLog("Launching " + profiles.size() + " profil...", "default");

        showProfileList(true);
        updateStats();

        new Thread(() -> {
            int jedaProfil = prefs.getJedaProfil() * 1000;
            for (int i = 0; i < profiles.size(); i++) {
                ProfileModel p = profiles.get(i);
                if (i > 0) {
                    try { Thread.sleep(jedaProfil); } catch (InterruptedException ignored) {}
                }
                p.status = "starting";
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    appendLog("Membuka " + p.packageName + "...", "default");
                });

                if (!p.cookie.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        appendLog("Injecting cookie: " + p.packageName + "...", "default"));
                    
                    boolean cookieOk = RobloxHelper.injectCookie(p.packageName, p.cookie);
                    final boolean fCookieOk = cookieOk;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (fCookieOk) {
                            appendLog("✓ Cookie injected: " + p.packageName, "green");
                        } else {
                            appendLog("✗ Cookie gagal (cek akses root): " + p.packageName, "red");
                        }
                    });
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }

                String link = !p.link.isEmpty() ? p.link : prefs.getDefaultLink();
                boolean ok = RobloxHelper.launchRoblox(requireContext(), p.packageName, link);
                p.status = ok ? "running" : "error";

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (ok) {
                        appendLog("✓ " + p.packageName + " → joined", "green");
                    } else {
                        appendLog("✗ " + p.packageName + " gagal launch", "red");
                    }
                    updateProfileList();
                    updateStats();
                });
            }

            prefs.saveProfiles(profiles);
            new Handler(Looper.getMainLooper()).post(() -> {
                setStatus("RUNNING", "#4ADE80");
                startMonitorService();
            });
        }).start();
    }

    private void handleStop() {
        if (!isRunning) return;
        isRunning = false;

        requireContext().stopService(new Intent(requireContext(), MonitorService.class));

        for (ProfileModel p : profiles) p.status = "idle";
        prefs.saveProfiles(profiles);

        setStatus("STOPPED", "#7070A0");
        showProfileList(false);
        updateStats();
        appendLog("Semua profil dihentikan.", "red");
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(requireContext(), MonitorService.class);
        requireContext().startForegroundService(serviceIntent);
    }

    private void setStatus(String text, String color) {
        statusText.setText(text);
        statusText.setTextColor(Color.parseColor(color));
        statusDot.setTextColor(Color.parseColor(color));
    }

    private void updateStats() {
        if (profiles == null) profiles = prefs.loadProfiles();
        int activeCount = 0;
        for (ProfileModel p : profiles) {
            if ("running".equals(p.status)) activeCount++;
        }
        statProfiles.setText(activeCount + "/" + profiles.size());
        statRestarts.setText(String.valueOf(restartCount));
        statDetector.setText(isRunning ? "1" : "—");
    }

    private void showProfileList(boolean show) {
        profileListContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        updateProfileList();
    }

    private void updateProfileList() {
        if (getContext() == null) return;
        profileListContainer.removeAllViews();
        for (ProfileModel p : profiles) {
            String dotColor = "running".equals(p.status) ? "#4ADE80"
                : "error".equals(p.status) ? "#F87171"
                : "#FBBF24";

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            TextView dot = new TextView(requireContext());
            dot.setText("●");
            dot.setTextColor(Color.parseColor(dotColor));
            dot.setTextSize(10);
            row.addView(dot);

            TextView name = new TextView(requireContext());
            name.setText("  " + p.packageName);
            name.setTextColor(Color.parseColor("#E8E8F0"));
            name.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(lp);
            row.addView(name);

            TextView status = new TextView(requireContext());
            status.setText(p.status);
            status.setTextColor(Color.parseColor("#7070A0"));
            status.setTextSize(11);
            row.addView(status);

            profileListContainer.addView(row);
        }
    }

    private void appendLog(String msg, String type) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            String time = timeFormat.format(new Date());
            String fullLine = "[" + time + "] " + msg + "\n";

            int startLen = logBuilder.length();
            logBuilder.append(fullLine);

            logBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#7070A0")),
                startLen, startLen + time.length() + 2, 0);

            int msgColor;
            switch (type) {
                case "green": msgColor = Color.parseColor("#4ADE80"); break;
                case "red": msgColor = Color.parseColor("#F87171"); break;
                case "yellow": msgColor = Color.parseColor("#FBBF24"); break;
                case "accent": msgColor = Color.parseColor("#7C6FF7"); break;
                default: msgColor = Color.parseColor("#E8E8F0"); break;
            }
            logBuilder.setSpan(new ForegroundColorSpan(msgColor),
                startLen + time.length() + 2, logBuilder.length(), 0);

            logText.setText(logBuilder);
            
            // Simpan Log Terenkode HTML ke Prefs
            prefs.saveLogs(Html.toHtml(logBuilder, Html.TO_HTML_MODE_LEGACY));
            
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }
}

package com.dhub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class MonitorService extends Service {

    public static final String ACTION_LOG = "com.dhub.LOG";
    public static final String ACTION_RESTART_COUNT = "com.dhub.RESTART_COUNT";
    public static final String EXTRA_LOG_MSG = "log_msg";
    public static final String EXTRA_LOG_TYPE = "log_type";
    public static final String EXTRA_RESTART_COUNT = "restart_count";

    private static final String CHANNEL_ID = "dhub_monitor";
    private static final int NOTIF_ID = 1;

    private AppPrefs prefs;
    private List<ProfileModel> profiles;
    private Handler handler;
    private Runnable monitorRunnable;
    private boolean running = false;
    private int totalRestarts = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new AppPrefs(this);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        profiles = prefs.loadProfiles();
        startForeground(NOTIF_ID, buildNotification());
        startMonitoring();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        if (handler != null && monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
        }
        super.onDestroy();
    }

    // ── Monitoring loop ───────────────────────────────────────────────────────
    private void startMonitoring() {
        running = true;
        int jedaCek = prefs.getJedaCek() * 1000;

        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                checkAllProfiles();
                handler.postDelayed(this, jedaCek);
            }
        };
        handler.postDelayed(monitorRunnable, jedaCek);
    }

    private void checkAllProfiles() {
        profiles = prefs.loadProfiles(); // reload latest
        int activeCount = 0;

        for (ProfileModel profile : profiles) {
            if (!"running".equals(profile.status)) continue;

            boolean isAlive = RobloxHelper.isPackageRunning(this, profile.packageName);
            if (isAlive) {
                activeCount++;
            } else {
                // Detected crash/kick → rejoin
                sendLog("⚠ " + profile.packageName + " tidak merespons, rejoining...", "yellow");
                new Thread(() -> rejoin(profile)).start();
            }
        }

        // Update notification
        updateNotification(activeCount);
    }

    private void rejoin(ProfileModel profile) {
        try {
            int jedaRestart = prefs.getJedaRestart() * 1000;

            // Force close dulu kalau setting aktif
            if (prefs.getForceClose()) {
                RobloxHelper.forceStop(this, profile.packageName);
                Thread.sleep(2000);
            }

            Thread.sleep(jedaRestart);

            // Inject cookie
            if (!profile.cookie.isEmpty()) {
                RobloxHelper.injectCookie(profile.packageName, profile.cookie);
                Thread.sleep(1500);
            }

            // Launch
            String link = !profile.link.isEmpty() ? profile.link : prefs.getDefaultLink();
            boolean success = RobloxHelper.launchRoblox(this, profile.packageName, link);

            totalRestarts++;

            if (success) {
                sendLog("✓ " + profile.packageName + " berhasil rejoin (#" + totalRestarts + ")", "green");
            } else {
                sendLog("✗ " + profile.packageName + " gagal rejoin", "red");
            }

            sendRestartCount(totalRestarts);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void sendLog(String msg, String type) {
        Intent intent = new Intent(ACTION_LOG);
        intent.putExtra(EXTRA_LOG_MSG, msg);
        intent.putExtra(EXTRA_LOG_TYPE, type);
        sendBroadcast(intent);
    }

    private void sendRestartCount(int count) {
        Intent intent = new Intent(ACTION_RESTART_COUNT);
        intent.putExtra(EXTRA_RESTART_COUNT, count);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "DHub Monitor", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Monitoring Roblox profiles");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DHub aktif")
            .setContentText("Memantau " + profiles.size() + " profil Roblox")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void updateNotification(int activeCount) {
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DHub aktif")
            .setContentText(activeCount + "/" + profiles.size() + " profil berjalan")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, notif);
    }
}

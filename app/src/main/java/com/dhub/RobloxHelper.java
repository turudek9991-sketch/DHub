package com.dhub;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.CookieManager;
import java.util.ArrayList;
import java.util.List;

public class RobloxHelper {

    /**
     * Scan semua package yang terinstall di device, filter yang mengandung "roblox"
     * Ini yang dipakai untuk auto-detect clone Roblox
     */
    public static List<String> detectRobloxPackages(Context context) {
        List<String> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        for (PackageInfo pkg : packages) {
            if (pkg.packageName.toLowerCase().contains("roblox")) {
                result.add(pkg.packageName);
            }
        }
        return result;
    }

    /**
     * Inject cookie .ROBLOSECURITY langsung ke database WebView Roblox app
     * via akses root. Ini diperlukan karena CookieManager biasa tidak bisa
     * menembus sandboxing antar-package di Android.
     *
     * @param packageName package Roblox target (mis: com.roblox.cliena)
     * @param cookie      nilai cookie .ROBLOSECURITY
     * @return true kalau berhasil
     */
    public static boolean injectCookie(String packageName, String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) return false;
        return RootCookieInjector.injectCookie(packageName, cookie.trim());
    }

    /**
     * Launch aplikasi Roblox via deep link (placeID join)
     * atau buka package langsung jika tidak ada link
     */
    public static boolean launchRoblox(Context context, String packageName, String link) {
        try {
            Intent intent;
            if (link != null && !link.trim().isEmpty()) {
                // Launch dengan deep link join game
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
                intent.setPackage(packageName);
            } else {
                // Launch main activity package
                PackageManager pm = context.getPackageManager();
                intent = pm.getLaunchIntentForPackage(packageName);
                if (intent == null) return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cek apakah package Roblox sedang berjalan di foreground/background
     * Kalau tidak jalan = crash/kick → perlu rejoin
     */
    public static boolean isPackageRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;
        for (ActivityManager.RunningAppProcessInfo proc : processes) {
            if (proc.processName != null && proc.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Force stop package (untuk force close sebelum restart)
     */
    public static void forceStop(Context context, String packageName) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}

package com.dhub;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
     */
    public static boolean injectCookie(String packageName, String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) return false;
        return RootCookieInjector.injectCookie(packageName, cookie.trim());
    }

    /**
     * PERBAIKAN FINAL LAUNCH: Memperbaiki intent agar aplikasi bisa terbuka kembali secara resmi
     * Menghilangkan konflik penulisan flag action/category yang merusak intent utama
     */
    public static boolean launchRoblox(Context context, String packageName, String link) {
        try {
            Intent intent = null;

            if (link != null && !link.trim().isEmpty()) {
                // Jika ada link game, jalankan Deep Link standar yang bersih
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
                intent.setPackage(packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                // Ambil intent peluncur resmi bawaan sistem paket Android
                PackageManager pm = context.getPackageManager();
                intent = pm.getLaunchIntentForPackage(packageName);
                
                if (intent == null) {
                    // Taktik Fallback: Jika sistem gagal, tembak langsung Component Activity Roblox secara eksplisit
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setComponent(new ComponentName(packageName, "com.roblox.client.Activity"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                } else {
                    // Jika intent resmi ditemukan, cukup tambahkan flag task baru agar stabil
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }

            // Eksekusi pemanggilan aktivitas ke sistem Android
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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

package com.dhub;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * RootCookieInjector
 *
 * Inject cookie .ROBLOSECURITY langsung ke database WebView Roblox
 * menggunakan akses root. Ini diperlukan karena Android App Sandboxing
 * mencegah CookieManager biasa "menembus" ke proses app lain.
 */
public class RootCookieInjector {

    /** Pesan error terakhir, bisa diambil setelah injectCookie() gagal */
    public static String lastError = "";

    public static boolean injectCookie(String packageName, String cookieValue) {
        lastError = "";
        try {
            // 0. Cek root dulu, kasih pesan jelas kalau tidak ada
            if (!isRootAvailable()) {
                lastError = "Akses root ditolak / tidak tersedia. Pastikan popup izin root di-Allow.";
                return false;
            }

            // 1. Pre-launch Roblox supaya WebView create database kosong
            execRootCommand("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
            sleep(4000);  // Tunggu WebView fully initialized

            // 2. Force-stop untuk unlock database
            execRootCommand("am force-stop " + packageName);
            sleep(1000);

            // 3. Path WebView cookie database Roblox
            String cookieDbPath = "/data/data/" + packageName + "/app_webview/Default/Cookies";

            // 4. Cek apakah file ada sekarang
            String checkResult = execRootCommandWithOutput("ls -la " + cookieDbPath + " 2>&1");
            if (checkResult == null) {
                lastError = "Tidak bisa menjalankan command root sama sekali.";
                return false;
            }
            if (checkResult.contains("No such file") || checkResult.trim().isEmpty()) {
                lastError = "File Cookies DB tidak ditemukan di: " + cookieDbPath;
                return false;
            }

            // 5. Hapus WAL dan SHM files (Android 10+) biar tidak overwrite database
            execRootCommand("rm -f \"" + cookieDbPath + "-wal\"");
            execRootCommand("rm -f \"" + cookieDbPath + "-shm\"");
            sleep(500);

            // 6. Cek sqlite3 tersedia
            String sqliteCheck = execRootCommandWithOutput("which sqlite3 2>&1");
            if (sqliteCheck == null || sqliteCheck.trim().isEmpty()) {
                lastError = "Command 'sqlite3' tidak ditemukan di device ini. Perlu install sqlite3 binary (lihat panduan).";
                return false;
            }

            // 7. Hapus cookie lama, insert yang baru
            String escapedCookie = cookieValue.replace("'", "''");
            long expiryMicros = (System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)) * 1000;
            long creationMicros = System.currentTimeMillis() * 1000;

            String sql =
                "DELETE FROM cookies WHERE name='.ROBLOSECURITY'; " +
                "INSERT INTO cookies (creation_utc, host_key, name, value, path, expires_utc, " +
                "is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, " +
                "encrypted_value, samesite, source_scheme, source_port, is_same_party) VALUES (" +
                creationMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " +
                expiryMicros + ", 1, 1, " + creationMicros + ", 1, 1, 1, '', -1, 2, 443, 0);";

            String command = "sqlite3 " + cookieDbPath + " \"" + sql + "\" 2>&1";
            String result = execRootCommandWithOutput(command);

            if (result != null && !result.trim().isEmpty() &&
                (result.toLowerCase().contains("error") || result.toLowerCase().contains("unable"))) {
                lastError = "SQLite error: " + result.trim();
                return false;
            }

            // 8. Fix permission
            execRootCommand("chmod 660 " + cookieDbPath);
            String uid = execRootCommandWithOutput("stat -c '%u:%g' /data/data/" + packageName + " 2>&1");
            if (uid != null && !uid.trim().isEmpty()) {
                execRootCommand("chown " + uid.trim() + " " + cookieDbPath);
            }

            return true;
        } catch (Exception e) {
            lastError = "Exception: " + e.getMessage();
            return false;
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static boolean execRootCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = process.waitFor();
            return result == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String execRootCommandWithOutput(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            process.waitFor();
            return result != null && result.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }
}

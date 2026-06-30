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
 *
 * Cara kerja:
 *   1. Stop dulu proses Roblox (biar file DB tidak locked)
 *   2. Cari lokasi cookie database Roblox app tersebut
 *   3. Tulis/replace cookie .ROBLOSECURITY pakai sqlite3 command via root shell
 *   4. (Opsional) set permission file biar tetap bisa dibaca app
 */
public class RootCookieInjector {

    /**
     * Inject cookie ke package Roblox tertentu via root.
     * Return true kalau command berhasil dieksekusi tanpa error.
     */
    public static boolean injectCookie(String packageName, String cookieValue) {
        try {
            // 1. Stop process dulu biar DB tidak ke-lock
            execRootCommand("am force-stop " + packageName);
            Thread.sleep(500);

            // 2. Path WebView cookie database Roblox (Chromium-based WebView)
            String cookieDbPath = "/data/data/" + packageName + "/app_webview/Default/Cookies";

            // 3. Cek apakah file ada, kalau belum ada (belum pernah dibuka), buat dummy dulu
            //    dengan membuka app sebentar lalu force-stop lagi
            String checkResult = execRootCommandWithOutput("ls " + cookieDbPath);
            if (checkResult == null || checkResult.contains("No such file")) {
                // Buka dulu app supaya WebView buat database-nya
                execRootCommand("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
                Thread.sleep(3000);
                execRootCommand("am force-stop " + packageName);
                Thread.sleep(500);
            }

            // 4. Hapus cookie .ROBLOSECURITY lama (kalau ada), lalu insert yang baru
            //    Format SQL ini mengikuti skema tabel "cookies" Chromium WebView
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

            String command = "sqlite3 " + cookieDbPath + " \"" + sql + "\"";
            boolean ok = execRootCommand(command);

            // 5. Fix permission supaya app bisa baca file-nya lagi
            execRootCommand("chmod 660 " + cookieDbPath);
            execRootCommand("chown $(stat -c '%u:%g' /data/data/" + packageName + ") " + cookieDbPath);

            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helper: eksekusi command via su, tanpa return output ────────────────
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

    // ── Helper: eksekusi command via su, dengan output ───────────────────────
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
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cek apakah device punya akses root yang berfungsi.
     */
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

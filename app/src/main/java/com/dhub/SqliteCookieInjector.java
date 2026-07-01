package com.dhub;

import android.app.ActivityManager;
import android.content.Context;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SqliteCookieInjector
 *
 * Inject cookie .ROBLOSECURITY via akses Java SQLite langsung
 * (tidak memerlukan binary sqlite3 di shell)
 *
 * Cara kerja:
 * 1. Pre-launch Roblox supaya WebView buat database
 * 2. Force-stop dan hapus WAL/SHM
 * 3. Akses database pakai java.sql.Connection (via root)
 * 4. Insert cookie langsung ke tabel
 */
public class SqliteCookieInjector {

    public static String lastError = "";

    public static boolean injectCookie(String packageName, String cookieValue) {
        lastError = "";
        try {
            if (cookieValue == null || cookieValue.trim().isEmpty()) {
                lastError = "Cookie value kosong";
                return false;
            }

            // 1. Pre-launch Roblox
            execRootCommand("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
            sleep(4000);

            // 2. Force-stop
            execRootCommand("am force-stop " + packageName);
            sleep(1000);

            // 3. Path database
            String dbPath = "/data/data/" + packageName + "/app_webview/Default/Cookies";

            // 4. Cek file ada
            String checkCmd = "ls -la " + dbPath + " 2>&1";
            String result = execRootCommandWithOutput(checkCmd);
            if (result == null || result.contains("No such file")) {
                lastError = "Database file tidak ditemukan: " + dbPath;
                return false;
            }

            // 5. Hapus WAL/SHM
            execRootCommand("rm -f \"" + dbPath + "-wal\"");
            execRootCommand("rm -f \"" + dbPath + "-shm\"");
            sleep(500);

            // 6. Copy database ke temp untuk akses via Java (biar tidak locked)
            String tempDb = "/data/local/tmp/cookies_temp.db";
            execRootCommand("cp \"" + dbPath + "\" \"" + tempDb + "\"");
            execRootCommand("chmod 666 \"" + tempDb + "\"");
            sleep(500);

            // 7. Inject cookie via Java SQLite
            boolean injected = injectViaSql(tempDb, cookieValue);

            if (!injected) {
                lastError = "SQLite injection gagal";
                return false;
            }

            // 8. Copy kembali ke lokasi asli (via root)
            execRootCommand("cp \"" + tempDb + "\" \"" + dbPath + "\"");
            execRootCommand("chmod 660 \"" + dbPath + "\"");

            // 9. Cek permission dan owner
            String uid = execRootCommandWithOutput("stat -c '%u:%g' /data/data/" + packageName);
            if (uid != null && !uid.trim().isEmpty()) {
                execRootCommand("chown " + uid.trim() + " \"" + dbPath + "\"");
            }

            // 10. Bersihkan temp
            execRootCommand("rm -f \"" + tempDb + "\"");

            return true;

        } catch (Exception e) {
            lastError = "Exception: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Akses SQLite database via JDBC dan inject cookie
     */
    private static boolean injectViaSql(String dbPath, String cookieValue) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            String escapedCookie = cookieValue.replace("'", "''");
            long creationMicros = System.currentTimeMillis() * 1000;
            long expiryMicros = creationMicros + (365L * 24 * 60 * 60 * 1000 * 1000);

            String sql = "DELETE FROM cookies WHERE name='.ROBLOSECURITY';" +
                    "INSERT OR REPLACE INTO cookies (creation_utc, host_key, name, value, path, expires_utc, " +
                    "is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, samesite, " +
                    "source_scheme, source_port) VALUES (" +
                    creationMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " +
                    expiryMicros + ", 1, 1, " + creationMicros + ", 1, 1, 1, -1, 2, 443);";

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();

            return true;
        } catch (Exception e) {
            lastError = "SQL Error: " + e.getMessage();
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
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
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

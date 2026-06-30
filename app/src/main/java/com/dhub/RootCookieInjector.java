package com.dhub;

import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import java.io.File;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootCookieInjector {

    public static boolean injectCookie(String packageName, String cookieValue) {
        String cookieDbPath = "/data/data/" + packageName + "/app_webview/Default/Cookies";
        // Alternatif lokasi jika path di atas gagal
        String altCookieDbPath = "/data/data/" + packageName + "/app_webview/Cookies";

        try {
            // 1. Force stop aplikasi target agar DB dilepas oleh Roblox
            execRootCommand("am force-stop " + packageName);
            Thread.sleep(500);

            // Cek lokasi database yang valid
            String targetPath = cookieDbPath;
            String check = execRootCommandWithOutput("ls " + cookieDbPath);
            if (check == null || check.contains("No such file")) {
                targetPath = altCookieDbPath;
            }

            // 2. LONGGARIN PERMISSION VIA ROOT (Agar Java internal DHub bisa baca-tulis file data Roblox)
            // Kita beri akses membaca ke direktori dan file database secara temporal
            execRootCommand("chmod 777 /data/data/" + packageName);
            execRootCommand("chmod 777 /data/data/" + packageName + "/app_webview");
            execRootCommand("chmod -R 777 " + new File(targetPath).getParent());
            execRootCommand("chmod 666 " + targetPath);

            // 3. MANIPULASI DATABASE MENGGUNAKAN SQLITE JAVA (Lebih aman dari ketergantungan binary sistem)
            File dbFile = new File(targetPath);
            if (!dbFile.exists()) {
                return false; 
            }

            // Buka database secara langsung menggunakan library bawaan framework Android
            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            
            // Hapus cookie lama
            db.delete("cookies", "name = ?", new String[]{".ROBLOSECURITY"});

            // Siapkan data baru (Android ContentValues secara otomatis mengamankan query dari broken strings)
            long currentMicros = System.currentTimeMillis() * 1000;
            long expiryMicros = currentMicros + (365L * 24 * 60 * 60 * 1000 * 1000);

            ContentValues values = new ContentValues();
            values.put("creation_utc", currentMicros);
            values.put("host_key", ".roblox.com");
            values.put("name", ".ROBLOSECURITY");
            values.put("value", cookieValue);
            values.put("path", "/");
            values.put("expires_utc", expiryMicros);
            values.put("is_secure", 1);
            values.put("is_httponly", 1);
            values.put("last_access_utc", currentMicros);
            values.put("has_expires", 1);
            values.put("is_persistent", 1);
            values.put("priority", 1);
            values.put("samesite", -1);
            values.put("source_scheme", 2);
            values.put("source_port", 443);

            long result = db.insert("cookies", null, values);
            db.close();

            // 4. KEMBALIKAN PERMISSION ASLI ROBLOX (Sangat krusial agar Roblox tidak crash saat dibuka)
            execRootCommand("chmod 751 /data/data/" + packageName);
            execRootCommand("chmod -R 700 /data/data/" + packageName + "/app_webview");
            execRootCommand("chmod 660 " + targetPath);
            // Kembalikan kepemilikan user ID bawaan aplikasi aslinya
            execRootCommand("chown -R $(stat -c '%u:%g' /data/data/" + packageName + ") /data/data/" + packageName);

            return result != -1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
}

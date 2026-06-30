package com.dhub;

import java.io.DataOutputStream;
import java.io.File;

/**
 * RootCookieInjector
 *
 * Inject cookie .ROBLOSECURITY langsung ke database WebView Roblox
 * menggunakan akses root. Ini diperlukan karena Android App Sandboxing
 * mencegah CookieManager biasa "menembus" ke proses app lain.
 *
 * Perbaikan Android 10: Menghapus berkas transaksi WAL (-wal dan -shm) 
 * agar Roblox dipaksa membaca data cookie baru dari database utama.
 */
public class RootCookieInjector {

    /**
     * Inject cookie ke package Roblox tertentu via root dan jalankan aktivitasnya.
     * Return true kalau command berhasil dieksekusi tanpa error.
     */
    public static boolean injectCookie(String packageName, String cookieValue) {
        Process process = null;
        DataOutputStream os = null;

        try {
            // 1. Tentukan path database cookies Roblox
            String cookieDbPath = "/data/data/" + packageName + "/app_webview/Default/Cookies";
            String altDbPath = "/data/data/" + packageName + "/app_webview/Cookies";

            // 2. Bersihkan karakter string cookie agar aman di shell
            String escapedCookie = cookieValue.replace("'", "''");
            long creationMicros = System.currentTimeMillis() * 1000;
            long expiryMicros = creationMicros + (365L * 24 * 60 * 60 * 1000 * 1000);

            // SQL Statement untuk menghapus dan memasukkan cookie baru
            String sql = "DELETE FROM cookies WHERE name='.ROBLOSECURITY'; " +
                         "INSERT OR REPLACE INTO cookies (creation_utc, host_key, name, value, path, expires_utc, is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, samesite, source_scheme, source_port) " +
                         "VALUES (" + creationMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " + expiryMicros + ", 1, 1, " + creationMicros + ", 1, 1, 1, -1, 2, 443);";

            // 3. Membuka satu sesi root shell terisolasi
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // Tulis script otomatis langsung dari kodingan Java ke area temporer
            os.writeBytes("cat << 'EOF' > /data/local/tmp/inject.sh\n");
            os.writeBytes("#!/system/bin/sh\n");
            os.writeBytes("setenforce 0\n"); // Matikan SELinux Android 10 sementara
            os.writeBytes("am force-stop " + packageName + "\n");
            
            // Logika pemilihan path database di dalam shell murni
            os.writeBytes("DB_PATH=\"" + cookieDbPath + "\"\n");
            os.writeBytes("[ ! -f \"$DB_PATH\" ] && DB_PATH=\"" + altDbPath + "\"\n");
            
            // PERBAIKAN SEBELUM INJEKSI: Hapus berkas WAL dan SHM lama jika ada
            // Ini agar Android tidak menimpa balik database utama dengan data sesi kosong
            os.writeBytes("rm -f \"${DB_PATH}-wal\"\n");
            os.writeBytes("rm -f \"${DB_PATH}-shm\"\n");
            
            // Eksekusi penulisan database via sqlite3 bawaan Redfinger
            os.writeBytes("sqlite3 \"$DB_PATH\" \"" + sql + "\"\n");
            
            // Kembalikan permission dan kepemilikan user ID Roblox (Wajib di Android 10)
            os.writeBytes("chmod 660 \"$DB_PATH\"\n");
            os.writeBytes("chown $(stat -c '%u:%g' /data/data/" + packageName + ") \"$DB_PATH\"\n");
            os.writeBytes("setenforce 1\n"); // Hidupkan SELinux kembali
            os.writeBytes("EOF\n");
            os.flush();

            // 4. JALANKAN SCRIPT SEBAGAI ROOT MURNI
            os.writeBytes("chmod +x /data/local/tmp/inject.sh\n");
            os.writeBytes("/data/local/tmp/inject.sh\n");
            
            // 5. Bersihkan kembali sisa script agar tidak menumpuk di memori
            os.writeBytes("rm /data/local/tmp/inject.sh\n");
            os.writeBytes("exit\n");
            os.flush();

            int exitVal = process.waitFor();
            
            // Jika exitVal adalah 0, shell sukses mengeksekusi script secara independen!
            return exitVal == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally { // <- Perbaikan penulisan kata kunci block (finally)
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Cek apakah device punya akses root yang berfungsi.
     */
    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitVal = process.waitFor();
            return exitVal == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

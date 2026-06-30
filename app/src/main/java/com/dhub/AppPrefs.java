package com.dhub;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppPrefs {
    private static final String PREF_NAME = "dhub_prefs";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_DEFAULT_LINK = "default_link";
    private static final String KEY_JEDA_CEK = "jeda_cek";
    private static final String KEY_JEDA_RESTART = "jeda_restart";
    private static final String KEY_DELAY_DETEKSI = "delay_deteksi";
    private static final String KEY_JEDA_PROFIL = "jeda_profil";
    private static final String KEY_FORCE_CLOSE = "force_close";
    private static final String KEY_DETECT_INGAME = "detect_ingame";
    private static final String KEY_DETECT_ERROR = "detect_error";
    private static final String KEY_VERIFY_JOIN = "verify_join";
    private static final String KEY_MODE_PERFORMA = "mode_performa";
    private static final String KEY_BERSIHKAN_CACHE = "bersihkan_cache";
    private static final String KEY_BEBASKAN_MEMORI = "bebaskan_memori";
    private static final String KEY_TUTUP_APLIKASI = "tutup_aplikasi";
    private static final String KEY_DISCORD_NOTIF = "discord_notif";
    private static final String KEY_WEBHOOK_URL = "webhook_url";
    private static final String KEY_LOGS_CACHE = "logs_cache"; // Tambahan Key Log

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public AppPrefs(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Logs Persistence ──────────────────────────────────────────────────────
    public void saveLogs(String logsText) {
        prefs.edit().putString(KEY_LOGS_CACHE, logsText).apply();
    }

    public String loadLogs() {
        return prefs.getString(KEY_LOGS_CACHE, "");
    }

    public void clearLogs() {
        prefs.edit().remove(KEY_LOGS_CACHE).apply();
    }

    // ── Profiles ─────────────────────────────────────────────────────────────
    public void saveProfiles(List<ProfileModel> profiles) {
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply();
    }

    public List<ProfileModel> loadProfiles() {
        String json = prefs.getString(KEY_PROFILES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<ProfileModel>>(){}.getType();
        try {
            List<ProfileModel> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    public String getDefaultLink() { return prefs.getString(KEY_DEFAULT_LINK, ""); }
    public void setDefaultLink(String v) { prefs.edit().putString(KEY_DEFAULT_LINK, v).apply(); }

    public int getJedaCek() { return prefs.getInt(KEY_JEDA_CEK, 10); }
    public void setJedaCek(int v) { prefs.edit().putInt(KEY_JEDA_CEK, v).apply(); }

    public int getJedaRestart() { return prefs.getInt(KEY_JEDA_RESTART, 5); }
    public void setJedaRestart(int v) { prefs.edit().putInt(KEY_JEDA_RESTART, v).apply(); }

    public int getDelayDeteksi() { return prefs.getInt(KEY_DELAY_DETEKSI, 45); }
    public void setDelayDeteksi(int v) { prefs.edit().putInt(KEY_DELAY_DETEKSI, v).apply(); }

    public int getJedaProfil() { return prefs.getInt(KEY_JEDA_PROFIL, 3); }
    public void setJedaProfil(int v) { prefs.edit().putInt(KEY_JEDA_PROFIL, v).apply(); }

    public boolean getForceClose() { return prefs.getBoolean(KEY_FORCE_CLOSE, true); }
    public void setForceClose(boolean v) { prefs.edit().putBoolean(KEY_FORCE_CLOSE, v).apply(); }

    public boolean getDetectInGame() { return prefs.getBoolean(KEY_DETECT_INGAME, true); }
    public void setDetectInGame(boolean v) { prefs.edit().putBoolean(KEY_DETECT_INGAME, v).apply(); }

    public boolean getDetectError() { return prefs.getBoolean(KEY_DETECT_ERROR, true); }
    public void setDetectError(boolean v) { prefs.edit().putBoolean(KEY_DETECT_ERROR, v).apply(); }

    public boolean getVerifyJoin() { return prefs.getBoolean(KEY_VERIFY_JOIN, true); }
    public void setVerifyJoin(boolean v) { prefs.edit().putBoolean(KEY_VERIFY_JOIN, v).apply(); }

    public boolean getModePerforma() { return prefs.getBoolean(KEY_MODE_PERFORMA, true); }
    public void setModePerforma(boolean v) { prefs.edit().putBoolean(KEY_MODE_PERFORMA, v).apply(); }

    public boolean getBersihkanCache() { return prefs.getBoolean(KEY_BERSIHKAN_CACHE, true); }
    public void setBersihkanCache(boolean v) { prefs.edit().putBoolean(KEY_BERSIHKAN_CACHE, v).apply(); }

    public boolean getBebaskanMemori() { return prefs.getBoolean(KEY_BEBASKAN_MEMORI, true); }
    public void setBebaskanMemori(boolean v) { prefs.edit().putBoolean(KEY_BEBASKAN_MEMORI, v).apply(); }

    public boolean getTutupAplikasi() { return prefs.getBoolean(KEY_TUTUP_APLIKASI, false); }
    public void setTutupAplikasi(boolean v) { prefs.edit().putBoolean(KEY_TUTUP_APLIKASI, v).apply(); }

    public boolean getDiscordNotif() { return prefs.getBoolean(KEY_DISCORD_NOTIF, false); }
    public void setDiscordNotif(boolean v) { prefs.edit().putBoolean(KEY_DISCORD_NOTIF, v).apply(); }

    public String getWebhookUrl() { return prefs.getString(KEY_WEBHOOK_URL, ""); }
    public void setWebhookUrl(String v) { prefs.edit().putString(KEY_WEBHOOK_URL, v).apply(); }

    public void resetToDefault() {
        prefs.edit()
            .putString(KEY_DEFAULT_LINK, "")
            .putInt(KEY_JEDA_CEK, 10)
            .putInt(KEY_JEDA_RESTART, 5)
            .putInt(KEY_DELAY_DETEKSI, 45)
            .putInt(KEY_JEDA_PROFIL, 3)
            .putBoolean(KEY_FORCE_CLOSE, true)
            .putBoolean(KEY_DETECT_INGAME, true)
            .putBoolean(KEY_DETECT_ERROR, true)
            .putBoolean(KEY_VERIFY_JOIN, true)
            .putBoolean(KEY_MODE_PERFORMA, true)
            .putBoolean(KEY_BERSIHKAN_CACHE, true)
            .putBoolean(KEY_BEBASKAN_MEMORI, true)
            .putBoolean(KEY_TUTUP_APLIKASI, false)
            .putBoolean(KEY_DISCORD_NOTIF, false)
            .putString(KEY_WEBHOOK_URL, "")
            .apply();
    }
}

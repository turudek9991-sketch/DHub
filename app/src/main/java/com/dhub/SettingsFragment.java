package com.dhub;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.util.List;

public class SettingsFragment extends Fragment {

    private AppPrefs prefs;
    private List<ProfileModel> profiles;
    private LinearLayout profilesContainer;
    private TextView txtDetectedCount;
    private EditText inputNewPackage, inputDefaultLink;
    private EditText inputJedaCek, inputJedaRestart, inputDelayDeteksi, inputJedaProfil;
    private EditText inputWebhookUrl;

    // Setting rows
    private Switch switchForceClose, switchDetectIngame, switchDetectError, switchVerifyJoin;
    private Switch switchModePerforma, switchBersihkanCache, switchBebaskanMemori, switchTutupAplikasi;
    private Switch switchDiscordNotif;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPrefs(requireContext());
        profiles = prefs.loadProfiles();

        bindViews(view);
        loadValues();
        setupListeners(view);
        renderProfiles();
    }

    private void bindViews(View view) {
        profilesContainer = view.findViewById(R.id.profiles_container);
        txtDetectedCount = view.findViewById(R.id.txt_detected_count);
        inputNewPackage = view.findViewById(R.id.input_new_package);
        inputDefaultLink = view.findViewById(R.id.input_default_link);
        inputJedaCek = view.findViewById(R.id.input_jeda_cek);
        inputJedaRestart = view.findViewById(R.id.input_jeda_restart);
        inputDelayDeteksi = view.findViewById(R.id.input_delay_deteksi);
        inputJedaProfil = view.findViewById(R.id.input_jeda_profil);
        inputWebhookUrl = view.findViewById(R.id.input_webhook_url);
    }

    private void loadValues() {
        inputDefaultLink.setText(prefs.getDefaultLink());
        inputJedaCek.setText(String.valueOf(prefs.getJedaCek()));
        inputJedaRestart.setText(String.valueOf(prefs.getJedaRestart()));
        inputDelayDeteksi.setText(String.valueOf(prefs.getDelayDeteksi()));
        inputJedaProfil.setText(String.valueOf(prefs.getJedaProfil()));
        inputWebhookUrl.setText(prefs.getWebhookUrl());
        updateDetectedCount();
    }

    private void setupListeners(View view) {
        // Auto detect
        view.findViewById(R.id.btn_auto_detect).setOnClickListener(v -> autoDetect());

        // Add package
        view.findViewById(R.id.btn_add_package).setOnClickListener(v -> {
            String name = inputNewPackage.getText().toString().trim();
            if (name.isEmpty()) return;
            if (profiles.stream().anyMatch(p -> p.packageName.equals(name))) {
                Toast.makeText(requireContext(), "Package sudah ada", Toast.LENGTH_SHORT).show();
                return;
            }
            profiles.add(new ProfileModel(name));
            prefs.saveProfiles(profiles);
            inputNewPackage.setText("");
            renderProfiles();
        });

        // Text watchers untuk auto-save
        addTextWatcher(inputDefaultLink, val -> prefs.setDefaultLink(val));
        addNumberWatcher(inputJedaCek, val -> prefs.setJedaCek(val));
        addNumberWatcher(inputJedaRestart, val -> prefs.setJedaRestart(val));
        addNumberWatcher(inputDelayDeteksi, val -> prefs.setDelayDeteksi(val));
        addNumberWatcher(inputJedaProfil, val -> prefs.setJedaProfil(val));
        addTextWatcher(inputWebhookUrl, val -> prefs.setWebhookUrl(val));

        // Reset button
        view.findViewById(R.id.btn_reset).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Reset Settings")
                .setMessage("Semua settings akan direset ke default?")
                .setPositiveButton("Reset", (d, w) -> {
                    prefs.resetToDefault();
                    loadValues();
                    // Reset switches
                    setAllSwitches();
                    Toast.makeText(requireContext(), "Settings direset", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
        });

        // Setup setting rows setelah inflate
        view.post(this::setupSettingRows);
    }

    private void setupSettingRows() {
        if (getView() == null) return;

        // Recovery rows
        setupRow(R.id.row_force_close, "Force close", "Buka ulang Roblox kalau ketutup paksa / crash",
            prefs.getForceClose(), val -> prefs.setForceClose(val));
        setupRow(R.id.row_detect_ingame, "Deteksi dalam game", "Pantau kejadian langsung dari dalam game",
            prefs.getDetectInGame(), val -> prefs.setDetectInGame(val));
        setupRow(R.id.row_detect_error, "Deteksi error", "Sambung ulang otomatis saat ke-disconnect",
            prefs.getDetectError(), val -> prefs.setDetectError(val));
        setupRow(R.id.row_verify_join, "Verifikasi join", "Pastikan berhasil masuk ke game",
            prefs.getVerifyJoin(), val -> prefs.setVerifyJoin(val));

        // Performance rows
        setupRow(R.id.row_mode_performa, "Mode performa", "Optimasi perangkat sebelum main",
            prefs.getModePerforma(), val -> prefs.setModePerforma(val));
        setupRow(R.id.row_bersihkan_cache, "Bersihkan cache", "Hapus cache sistem & Roblox",
            prefs.getBersihkanCache(), val -> prefs.setBersihkanCache(val));
        setupRow(R.id.row_bebaskan_memori, "Bebaskan memori", "Kosongkan RAM sebelum main",
            prefs.getBebaskanMemori(), val -> prefs.setBebaskanMemori(val));
        setupRow(R.id.row_tutup_aplikasi, "Tutup aplikasi lain", "Lebih agresif bebasin RAM (hati-hati)",
            prefs.getTutupAplikasi(), val -> prefs.setTutupAplikasi(val));

        // Discord
        setupRow(R.id.row_discord_notif, "Notifikasi Discord", "Kirim status ke webhook",
            prefs.getDiscordNotif(), val -> {
                prefs.setDiscordNotif(val);
                if (inputWebhookUrl != null)
                    inputWebhookUrl.setVisibility(val ? View.VISIBLE : View.GONE);
            });

        if (inputWebhookUrl != null)
            inputWebhookUrl.setVisibility(prefs.getDiscordNotif() ? View.VISIBLE : View.GONE);
    }

    private void setupRow(int rowId, String title, String desc, boolean initialValue, OnToggleChange listener) {
        View rowView = getView() == null ? null : getView().findViewById(rowId);
        if (rowView == null) return;
        ((TextView) rowView.findViewById(R.id.row_title)).setText(title);
        ((TextView) rowView.findViewById(R.id.row_desc)).setText(desc);
        Switch sw = rowView.findViewById(R.id.row_switch);
        sw.setChecked(initialValue);
        sw.setOnCheckedChangeListener((btn, checked) -> listener.onChange(checked));
    }

    private void setAllSwitches() {
        setupSettingRows();
    }

    // ── Auto-detect packages ──────────────────────────────────────────────────
    private void autoDetect() {
        List<String> detected = RobloxHelper.detectRobloxPackages(requireContext());
        int added = 0;
        for (String pkg : detected) {
            boolean exists = profiles.stream().anyMatch(p -> p.packageName.equals(pkg));
            if (!exists) {
                profiles.add(new ProfileModel(pkg));
                added++;
            }
        }
        prefs.saveProfiles(profiles);
        renderProfiles();
        String msg = detected.isEmpty()
            ? "Tidak ada package Roblox ditemukan"
            : detected.size() + " package ditemukan, " + added + " baru ditambahkan";
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    // ── Render profile cards ──────────────────────────────────────────────────
    private void renderProfiles() {
        profilesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < profiles.size(); i++) {
            final int idx = i;
            ProfileModel p = profiles.get(i);
            View card = inflater.inflate(R.layout.item_profile_card, profilesContainer, false);

            ((TextView) card.findViewById(R.id.profile_title))
                .setText("Profil " + (i + 1) + " · " + p.packageName);

            EditText cookieInput = card.findViewById(R.id.input_cookie);
            cookieInput.setText(p.cookie);
            cookieInput.addTextChangedListener(new SimpleTextWatcher(val -> {
                profiles.get(idx).cookie = val;
                prefs.saveProfiles(profiles);
            }));

            EditText linkInput = card.findViewById(R.id.input_link);
            linkInput.setText(p.link);
            linkInput.addTextChangedListener(new SimpleTextWatcher(val -> {
                profiles.get(idx).link = val;
                prefs.saveProfiles(profiles);
            }));

            card.findViewById(R.id.btn_remove).setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Profil")
                    .setMessage("Hapus " + p.packageName + "?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        profiles.remove(idx);
                        prefs.saveProfiles(profiles);
                        renderProfiles();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            });

            profilesContainer.addView(card);
        }
        updateDetectedCount();
    }

    private void updateDetectedCount() {
        txtDetectedCount.setText(profiles.size() + " package terdeteksi → " + profiles.size() + " profil");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    interface OnToggleChange { void onChange(boolean val); }
    interface OnStringChange { void onChange(String val); }
    interface OnIntChange { void onChange(int val); }

    private void addTextWatcher(EditText et, OnStringChange listener) {
        et.addTextChangedListener(new SimpleTextWatcher(listener::onChange));
    }

    private void addNumberWatcher(EditText et, OnIntChange listener) {
        et.addTextChangedListener(new SimpleTextWatcher(val -> {
            try { listener.onChange(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
        }));
    }

    static class SimpleTextWatcher implements TextWatcher {
        private final OnStringChange listener;
        SimpleTextWatcher(OnStringChange listener) { this.listener = listener; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { listener.onChange(s.toString()); }
    }
}

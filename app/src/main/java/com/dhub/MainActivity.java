package com.dhub;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navDashboard, navSettings;
    private TextView navDashboardIcon, navDashboardLabel;
    private TextView navSettingsIcon, navSettingsLabel;

    private final DashboardFragment dashboardFragment = new DashboardFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private Fragment currentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // PERBAIKAN: Paksa aplikasi DHub selalu berjalan dalam mode Landscape murni semenjak awal
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navDashboard = findViewById(R.id.nav_dashboard);
        navSettings = findViewById(R.id.nav_settings);
        navDashboardIcon = findViewById(R.id.nav_dashboard_icon);
        navDashboardLabel = findViewById(R.id.nav_dashboard_label);
        navSettingsIcon = findViewById(R.id.nav_settings_icon);
        navSettingsLabel = findViewById(R.id.nav_settings_label);

        navDashboard.setOnClickListener(v -> switchTo("dashboard"));
        navSettings.setOnClickListener(v -> switchTo("settings"));

        // Buka dashboard default
        switchTo("dashboard");
    }

    private void switchTo(String tab) {
        Fragment target = tab.equals("dashboard") ? dashboardFragment : settingsFragment;

        if (currentFragment == target) return;

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (!target.isAdded()) {
            tx.add(R.id.fragment_container, target);
        }
        if (currentFragment != null) {
            tx.hide(currentFragment);
        }
        tx.show(target).commit();
        currentFragment = target;

        // Update nav colors
        boolean isDash = tab.equals("dashboard");
        int accentColor = getColor(R.color.accent);
        int mutedColor = getColor(R.color.text_muted);

        navDashboardIcon.setTextColor(isDash ? accentColor : mutedColor);
        navDashboardLabel.setTextColor(isDash ? accentColor : mutedColor);
        navDashboardLabel.setTypeface(null, isDash ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        navSettingsIcon.setTextColor(isDash ? mutedColor : accentColor);
        navSettingsLabel.setTextColor(isDash ? mutedColor : accentColor);
        navSettingsLabel.setTypeface(null, isDash ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
    }
}

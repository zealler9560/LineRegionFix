package io.muse.lineregionfix;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Settings UI modeled on the Nrfr layout: three dropdown rows
 * (SIM scope / country / carrier) plus reset & save buttons at the bottom.
 * Choices are stored in SharedPreferences "config"; the hook reads them
 * through XSharedPreferences inside the LINE process.
 */
public class SettingsActivity extends Activity {

    public static final String PREFS_NAME = "config";
    public static final String KEY_ISO = "iso";
    public static final String KEY_NUMERIC = "numeric";
    public static final String KEY_ALPHA = "alpha";
    public static final String KEY_TZ = "tz";
    public static final String KEY_LOCALE = "locale";
    public static final String KEY_SIM_SCOPE = "sim_scope"; // 0=all, 1=SIM1, 2=SIM2

    private Spinner simSpinner;
    private Spinner countrySpinner;
    private Spinner carrierSpinner;
    private TextView summaryView;

    private static final String[] SIM_LABELS = {"全部 SIM", "SIM 1", "SIM 2"};

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable boxBg() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.WHITE);
        d.setStroke(dp(1), Color.parseColor("#8a8a8a"));
        d.setCornerRadius(dp(6));
        return d;
    }

    private GradientDrawable btnBg(String fill, String stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(fill));
        if (stroke != null) {
            d.setStroke(dp(1), Color.parseColor(stroke));
        }
        d.setCornerRadius(dp(24));
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#f8faf9"));
        root.setPadding(dp(28), dp(36), dp(28), dp(24));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(new TextView(this), new LinearLayout.LayoutParams(
                dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = new TextView(this);
        title.setText("LineRegionFix");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#17352a"));
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView info = new TextView(this);
        info.setText("ⓘ");
        info.setTextSize(22);
        info.setTextColor(Color.parseColor("#17352a"));
        info.setGravity(Gravity.CENTER);
        info.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        header.addView(info, new LinearLayout.LayoutParams(
                dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("LINE 地区信号伪装 · 仅作用于 LINE");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.parseColor("#6b7a73"));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        subLp.bottomMargin = dp(30);
        root.addView(subtitle, subLp);

        simSpinner = addRow(root, "选择 SIM 卡");
        countrySpinner = addRow(root, "选择国家码");
        carrierSpinner = addRow(root, "选择运营商");

        summaryView = new TextView(this);
        summaryView.setTextSize(13);
        summaryView.setTextColor(Color.parseColor("#40534a"));
        summaryView.setLineSpacing(0, 1.3f);
        LinearLayout.LayoutParams sumLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sumLp.topMargin = dp(10);
        root.addView(summaryView, sumLp);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button resetBtn = new Button(this);
        resetBtn.setText("还原设置");
        resetBtn.setTextSize(15);
        resetBtn.setTextColor(Color.parseColor("#5a6a62"));
        resetBtn.setBackground(btnBg("#ffffff", "#8a8a8a"));
        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        rbLp.rightMargin = dp(10);
        btnRow.addView(resetBtn, rbLp);

        Button saveBtn = new Button(this);
        saveBtn.setText("保存生效");
        saveBtn.setTextSize(15);
        saveBtn.setTextColor(Color.parseColor("#17352a"));
        saveBtn.setBackground(btnBg("#c3d6cc", null));
        btnRow.addView(saveBtn, new LinearLayout.LayoutParams(0, dp(50), 1f));

        root.addView(btnRow);
        setContentView(root);

        // --- populate spinners ---
        simSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, SIM_LABELS));

        String[] regionNames = new String[RegionPresets.REGIONS.length];
        for (int i = 0; i < regionNames.length; i++) {
            regionNames[i] = RegionPresets.REGIONS[i].name
                    + " (" + RegionPresets.REGIONS[i].iso.toUpperCase() + ")";
        }
        countrySpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, regionNames));

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fillCarriers(position, null);
                updateSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        carrierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        simSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        restoreSelections();

        saveBtn.setOnClickListener(v -> save());
        resetBtn.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            restoreSelections();
            Toast.makeText(this, "已还原为默认（日本 · NTT DOCOMO），强停 LINE 重开后生效",
                    Toast.LENGTH_LONG).show();
        });
    }

    private Spinner addRow(LinearLayout root, String hint) {
        Spinner sp = new Spinner(this);
        sp.setBackground(boxBg());
        sp.setPadding(dp(14), 0, dp(8), 0);
        sp.setPrompt(hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.bottomMargin = dp(22);
        root.addView(sp, lp);
        return sp;
    }

    private void fillCarriers(int regionIdx, String wantedNumeric) {
        RegionPresets.Region r = RegionPresets.REGIONS[regionIdx];
        String[] labels = new String[r.carriers.length];
        int sel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = r.carriers[i].alpha + " (" + r.carriers[i].numeric + ")";
            if (wantedNumeric != null && wantedNumeric.equals(r.carriers[i].numeric)) {
                sel = i;
            }
        }
        carrierSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        carrierSpinner.setSelection(sel);
    }

    private void restoreSelections() {
        SharedPreferences p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String iso = p.getString(KEY_ISO, RegionPresets.REGIONS[0].iso);
        String numeric = p.getString(KEY_NUMERIC, RegionPresets.REGIONS[0].carriers[0].numeric);
        int simScope = p.getInt(KEY_SIM_SCOPE, 0);

        int regionIdx = 0;
        for (int i = 0; i < RegionPresets.REGIONS.length; i++) {
            if (RegionPresets.REGIONS[i].iso.equals(iso)) {
                regionIdx = i;
                break;
            }
        }
        simSpinner.setSelection(simScope >= 0 && simScope <= 2 ? simScope : 0);
        countrySpinner.setSelection(regionIdx);
        fillCarriers(regionIdx, numeric);
        updateSummary();
    }

    private RegionPresets.Region currentRegion() {
        return RegionPresets.REGIONS[countrySpinner.getSelectedItemPosition()];
    }

    private RegionPresets.Carrier currentCarrier() {
        RegionPresets.Region r = currentRegion();
        int idx = carrierSpinner.getSelectedItemPosition();
        if (idx < 0 || idx >= r.carriers.length) {
            idx = 0;
        }
        return r.carriers[idx];
    }

    private void updateSummary() {
        if (countrySpinner.getSelectedItemPosition() < 0 || carrierSpinner.getAdapter() == null) {
            return;
        }
        RegionPresets.Region r = currentRegion();
        RegionPresets.Carrier c = currentCarrier();
        int sim = simSpinner.getSelectedItemPosition();
        summaryView.setText("生效值：国家码 " + r.iso
                + " · 运营商 " + c.numeric + " / " + c.alpha
                + "\n时区 " + r.timezoneId + " · 语言地区 " + r.localeTag
                + " · 范围 " + SIM_LABELS[sim < 0 ? 0 : sim]
                + "\n保存后强停 LINE 并重新打开即可生效。");
    }

    private void save() {
        RegionPresets.Region r = currentRegion();
        RegionPresets.Carrier c = currentCarrier();
        SharedPreferences.Editor e = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        e.putString(KEY_ISO, r.iso);
        e.putString(KEY_NUMERIC, c.numeric);
        e.putString(KEY_ALPHA, c.alpha);
        e.putString(KEY_TZ, r.timezoneId);
        e.putString(KEY_LOCALE, r.localeTag);
        e.putInt(KEY_SIM_SCOPE, simSpinner.getSelectedItemPosition());
        e.apply();
        // Best effort: let the hooked process read the prefs file on setups that need it.
        try {
            File f = new File(getApplicationInfo().dataDir + "/shared_prefs/" + PREFS_NAME + ".xml");
            if (f.exists()) {
                f.setReadable(true, false);
            }
        } catch (Throwable ignored) {
        }
        Toast.makeText(this, "已保存：" + r.name + " · " + c.alpha + "，强停 LINE 重开后生效",
                Toast.LENGTH_LONG).show();
    }
}

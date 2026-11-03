package io.muse.lineregionfix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * About page modeled on Nrfr's: feature-intro card, author card,
 * open-source card and a copyright footer. Wording is our own —
 * unlike Nrfr this module does require Root + LSPosed, and says so.
 */
public class AboutActivity extends Activity {

    private static final String REPO_URL = "https://github.com/zealler9560/LineRegionFix";
    private static final String AUTHOR_URL = "https://github.com/zealler9560";

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable cardBg() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor("#dfe9e4"));
        d.setCornerRadius(dp(14));
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#f8faf9"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        scroll.addView(root);

        // header: back arrow + centered title
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("←");
        back.setTextSize(22);
        back.setTextColor(Color.parseColor("#17352a"));
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = new TextView(this);
        title.setText("关于");
        title.setTextSize(20);
        title.setTextColor(Color.parseColor("#17352a"));
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(new TextView(this), new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.bottomMargin = dp(22);
        root.addView(header, headLp);

        // feature intro card
        root.addView(card("功能介绍", new String[]{
                "•  伪装 LINE 读取的设备地区信号（SIM/网络国家码、运营商、系统属性、时区、语言地区），可用于解除 LINE 的客户端地区限制",
                "•  地区可自选：日本、韩国、台湾、香港、泰国、美国、德国，运营商可进一步细分",
                "•  仅作用于 LINE 一个应用，不影响其他应用与系统本身",
                "•  需要 Root 与 LSPosed，启用时作用域只勾选 LINE",
                "•  随时可还原默认；停用模块即完全失效",
                "•  LINE 实际读取的每个信号都会记入 LSPosed 日志，便于核对生效情况",
        }, null));

        root.addView(sectionTitle("作者信息"));
        root.addView(card(null, new String[]{
                "作者：zealler9560",
                "GitHub：zealler9560",
        }, AUTHOR_URL));

        root.addView(sectionTitle("开源信息"));
        LinearLayout srcCard = card(null, null, null);
        TextView srcText = new TextView(this);
        srcText.setText("本项目已在 GitHub 开源");
        srcText.setTextSize(15);
        srcText.setTextColor(Color.parseColor("#2c3e36"));
        srcText.setGravity(Gravity.CENTER);
        srcCard.addView(srcText);
        TextView srcLink = new TextView(this);
        srcLink.setText("访问项目主页");
        srcLink.setTextSize(15);
        srcLink.setTextColor(Color.parseColor("#0f766e"));
        srcLink.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slLp.topMargin = dp(10);
        srcLink.setOnClickListener(v -> openUrl(REPO_URL));
        srcCard.addView(srcLink, slLp);
        root.addView(srcCard);

        TextView footer = new TextView(this);
        footer.setText("© 2026 zealler9560");
        footer.setTextSize(13);
        footer.setTextColor(Color.parseColor("#6b7a73"));
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.topMargin = dp(26);
        root.addView(footer, fLp);

        setContentView(scroll);
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(Color.parseColor("#17352a"));
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(22);
        lp.bottomMargin = dp(12);
        t.setLayoutParams(lp);
        return t;
    }

    /** A rounded card; optional bold heading inside; lines; whole card opens link if given. */
    private LinearLayout card(String heading, String[] lines, String link) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(cardBg());
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(6);
        c.setLayoutParams(clp);

        if (heading != null) {
            TextView h = new TextView(this);
            h.setText(heading);
            h.setTextSize(16);
            h.setTextColor(Color.parseColor("#17352a"));
            LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hLp.bottomMargin = dp(10);
            c.addView(h, hLp);
        }
        if (lines != null) {
            for (String line : lines) {
                TextView t = new TextView(this);
                t.setText(line);
                t.setTextSize(14);
                t.setTextColor(Color.parseColor("#2c3e36"));
                t.setLineSpacing(0, 1.35f);
                LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tLp.bottomMargin = dp(6);
                if (link != null && line.startsWith("GitHub")) {
                    t.setTextColor(Color.parseColor("#0f766e"));
                }
                c.addView(t, tLp);
            }
        }
        if (link != null) {
            c.setOnClickListener(v -> openUrl(link));
        }
        return c;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) {
        }
    }
}

package com.zeus97x.zbattle;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private MasterProfile profile;
    private LinearLayout root, content;
    private String tab = "Camp";
    private final int background = Color.rgb(12, 18, 30), panel = Color.rgb(24, 34, 51);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("zbattle.profile.v1", MODE_PRIVATE);
        if (state != null) tab = state.getString("tab", "Camp");
        if (prefs.contains("name")) {
            try { profile = new MasterProfile(prefs.getString("name", ""), prefs.getInt("style", 0),
                prefs.getInt("gender", 0), prefs.getInt("starter", 0)); }
            catch (IllegalArgumentException ignored) { profile = null; }
        }
        if (profile == null) setup(); else home();
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("tab", tab); super.onSaveInstanceState(out); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private void frame() {
        root = column(); root.setBackgroundColor(background); root.setPadding(dp(16), dp(12), dp(16), 0);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(16) + insets.getSystemWindowInsetLeft(), dp(12) + insets.getSystemWindowInsetTop(),
                dp(16) + insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
    }
    private void text(LinearLayout into, String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE);
        v.setPadding(0, dp(8), 0, dp(8)); into.addView(v);
    }
    private Spinner choice(LinearLayout into, String label, String[] values) {
        text(into, label, 15); Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values));
        into.addView(s); return s;
    }
    private void button(LinearLayout into, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run()); into.addView(b);
    }
    private void setup() {
        frame(); ScrollView scroll = new ScrollView(this); content = column();
        scroll.addView(content); root.addView(scroll);
        text(content, "ZBattle", 30); text(content, "Begin your journey", 24);
        text(content, "Choose your Pet Master. Every style can learn every skill.", 16);
        EditText name = new EditText(this); name.setHint("Pet Master name"); name.setTextColor(Color.WHITE);
        name.setHintTextColor(Color.LTGRAY); name.setSingleLine(true);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
        content.addView(name);
        Spinner style = choice(content, "Style", MasterProfile.STYLES);
        Spinner gender = choice(content, "Gender", MasterProfile.GENDERS);
        Spinner starter = choice(content, "Starter companion", MasterProfile.STARTERS);
        button(content, "Start adventure", () -> {
            try {
                MasterProfile selected = new MasterProfile(name.getText().toString(), style.getSelectedItemPosition(),
                    gender.getSelectedItemPosition(), starter.getSelectedItemPosition());
                boolean saved = prefs.edit().putString("name", selected.name).putInt("style", selected.style)
                    .putInt("gender", selected.gender).putInt("starter", selected.starter).commit();
                if (!saved) { Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_LONG).show(); return; }
                profile = selected; home();
            } catch (IllegalArgumentException e) { name.setError(e.getMessage()); }
        });
        text(content, "Character illustrations and gameplay arrive in later phases.", 14);
    }
    private LinearLayout card(String title, String description) {
        LinearLayout c = column(); c.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(panel); bg.setCornerRadius(dp(18)); c.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(12); content.addView(c, p);
        text(c, title, 21); text(c, description, 15); return c;
    }
    private void home() {
        frame(); text(root, "ZBattle", 28);
        ScrollView scroll = new ScrollView(this); content = column(); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if ("Camp".equals(tab)) {
            card("Welcome, " + profile.name, MasterProfile.STYLES[profile.style] + " · " + MasterProfile.GENDERS[profile.gender] + " · Level 1");
            LinearLayout companion = card(MasterProfile.STARTERS[profile.starter], "Your first companion · Battle level 1");
            ImageView image = new ImageView(this); image.setContentDescription(MasterProfile.STARTERS[profile.starter]);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            try (InputStream stream = getAssets().open("monsters/" + MasterProfile.STARTERS[profile.starter].toLowerCase(Locale.ROOT) + ".png")) {
                image.setImageDrawable(Drawable.createFromStream(stream, null)); companion.addView(image, new LinearLayout.LayoutParams(-1, dp(220)));
            } catch (java.io.IOException e) { text(companion, "Companion artwork unavailable", 14); }
            card("Your journey", "Explore with your companion and develop your Pet Master. Adventure and AFK tasks are being prepared.");
        } else if ("Adventure".equals(tab)) {
            card("Adventure", "Region exploration, encounters and battles are planned for the next gameplay phases.");
        } else if ("Tasks".equals(tab)) {
            card("Pet Master tasks", "Gathering, crafting and companion expeditions are planned. No AFK rewards are granted yet.");
        } else {
            card("Pet Master", profile.name + " · " + MasterProfile.STYLES[profile.style] + " · " + MasterProfile.GENDERS[profile.gender]);
            card("Skills and equipment", "All styles will share access to skills and equipment. These systems are planned.");
            card("ZPet connection", "Planned: copy a pet from ZPet into ZBattle with separate battle progress. ZBattle rewards never return to ZPet.");
        }
        LinearLayout nav = new LinearLayout(this);
        for (String label : new String[]{"Camp", "Adventure", "Tasks", "Master"}) {
            Button b = new Button(this); b.setText(label); b.setTextSize(11); b.setAllCaps(false);
            b.setContentDescription(label + ("".equals(label) ? "" : (label.equals(tab) ? ", selected" : "")));
            b.setOnClickListener(v -> { tab = label; home(); });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
        root.addView(nav);
    }
}

package home.projector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;

public class HomeActivity extends Activity {

    /** The big tiles, left to right. Edit this list to change the home screen. */
    private final Tile[] tiles = {
            new Tile("Netflix", "netflix",
                    "com.netflix.ninja", "com.netflix.mediaclient"),
            new Tile("YouTube", "youtube",
                    "com.google.android.youtube.tv",
                    "com.liskovsoft.smarttubetv.beta", "com.liskovsoft.smarttubetv",
                    "com.google.android.youtube"),
            new Tile("Prime Video", "prime",
                    "com.amazon.amazonvideo.livingroom",
                    "com.amazon.avod.thirdpartyclient", "com.amazon.avod"),
            new Tile("HDMI", "hdmi", "com.softwinner.awlivetv")
                    .withComponent("com.softwinner.awlivetv", "com.softwinner.awlivetv.MainActivity"),
    };

    /** Small buttons, bottom right. "Picture" is keystone; it falls back to display settings. */
    private final Tile picture = new Tile("Picture", "keystone")
            .withAction(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
    private final Tile settings = new Tile("Settings", null)
            .withAction(new Intent(Settings.ACTION_SETTINGS));

    private TextView[] tileViews;
    private ImageView background;
    private View scrim;
    private WallpaperStore wallpapers;
    private String loadedWallpaper;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        wallpapers = new WallpaperStore(this);

        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Ui.BG);

        // Wallpaper, with a dark veil so text stays readable on bright photos.
        background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        screen.addView(background, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        scrim = new View(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setVisibility(View.GONE);
        screen.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout root = new FrameLayout(this);
        int side = Ui.dp(this, 56);
        root.setPadding(side, Ui.dp(this, 40), side, Ui.dp(this, 36));
        screen.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        root.addView(clock(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        tileViews = new TextView[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            final Tile t = tiles[i];
            TextView v = button(t.label, 24, 16, 12);
            v.setOnClickListener(view -> open(t));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            int m = Ui.dp(this, 10);
            lp.setMargins(m, 0, m, 0);
            row.addView(v, lp);
            tileViews[i] = v;
        }
        root.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 150), Gravity.CENTER));

        LinearLayout small = new LinearLayout(this);
        small.setOrientation(LinearLayout.HORIZONTAL);
        small.addView(smallButton(picture.label, view -> open(picture)));
        small.addView(smallButton("Wallpaper", view ->
                Ui.launch(this, new Intent(this, WallpaperActivity.class), "Wallpaper")));
        small.addView(smallButton("All apps", view -> openApps()));
        small.addView(smallButton(settings.label, view -> open(settings)));
        root.addView(small, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END));

        setContentView(screen);
        tileViews[0].requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Dim any tile whose app isn't installed (re-checked each time, so new installs show up).
        for (int i = 0; i < tiles.length; i++) {
            tileViews[i].setAlpha(tiles[i].resolve(this) == null ? 0.35f : 1f);
        }
        loadWallpaper();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Pressing Home while already home jumps back to the first tile.
        tileViews[0].requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openApps();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return true; // stay on the home screen
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Home screen: nothing to go back to.
    }

    /** Loads the chosen picture off the main thread, only when it has changed. */
    private void loadWallpaper() {
        final File f = wallpapers.current();
        final String key = f == null ? "" : f.getName();
        if (key.equals(loadedWallpaper)) return;
        loadedWallpaper = key;

        if (f == null) {
            background.setImageDrawable(null);
            scrim.setVisibility(View.GONE);
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        final int w = Math.max(dm.widthPixels, dm.heightPixels);
        final int h = Math.min(dm.widthPixels, dm.heightPixels);
        new Thread(() -> {
            final Bitmap b = WallpaperStore.load(f, w, h);
            runOnUiThread(() -> {
                if (!key.equals(loadedWallpaper)) return;
                background.setImageBitmap(b);
                scrim.setVisibility(b == null ? View.GONE : View.VISIBLE);
            });
        }, "wallpaper").start();
    }

    private void open(Tile t) {
        Ui.launch(this, t.resolve(this), t.label);
    }

    private void openApps() {
        Ui.launch(this, new Intent(this, AppsActivity.class), "All apps");
    }

    private View clock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextColor(Ui.TEXT);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 52);
        time.setIncludeFontPadding(false);
        time.setShadowLayer(12, 0, 2, 0x99000000);
        box.addView(time);

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE d MMMM");
        date.setFormat24Hour("EEEE d MMMM");
        date.setTextColor(0xFFC8C8CC);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        date.setPadding(Ui.dp(this, 2), Ui.dp(this, 6), 0, 0);
        date.setShadowLayer(10, 0, 1, 0x99000000);
        box.addView(date);
        return box;
    }

    private TextView button(String text, float sp, float radiusDp, float padDp) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(Ui.focusText());
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setBackground(Ui.card(this, radiusDp, 3));
        int p = Ui.dp(this, padDp);
        v.setPadding(p * 2, p, p * 2, p);
        v.setFocusable(true);
        v.setClickable(true);
        return v;
    }

    private TextView smallButton(String text, View.OnClickListener l) {
        TextView v = button(text, 15, 22, 9);
        v.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        v.setLayoutParams(lp);
        return v;
    }
}

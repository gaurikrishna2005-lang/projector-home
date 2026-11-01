package home.projector;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Pick a background picture. While this screen is open, a phone on the same Wi-Fi
 * can send new pictures by visiting the address shown.
 */
public class WallpaperActivity extends Activity {

    private WallpaperStore store;
    private UploadServer server;
    private TextView address;
    private LinearLayout row;
    private int generation;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = new WallpaperStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int side = Ui.dp(this, 56);
        root.setPadding(side, Ui.dp(this, 36), side, Ui.dp(this, 24));

        root.addView(text("Wallpaper", 26, Ui.TEXT, 0));
        root.addView(text("To add photos, open this in your phone's browser (same Wi-Fi):",
                16, Ui.TEXT_DIM, 18));

        address = text("Starting…", 32, Ui.TEXT, 6);
        address.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        root.addView(address);

        root.addView(text("Your pictures   ·   OK to use   ·   hold OK to delete", 14, Ui.TEXT_DIM, 34));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row);
        root.addView(scroll);

        setContentView(root);

        server = new UploadServer(readPage(), store.tempFile(), raw -> {
            final File f = store.save(raw);
            if (f == null) return false;
            store.setCurrent(f);
            runOnUiThread(() -> {
                refresh(f);
                Toast.makeText(this, "New wallpaper set", Toast.LENGTH_SHORT).show();
            });
            return true;
        });

        refresh(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            int port = server.start();
            String ip = UploadServer.localIp();
            address.setText(ip == null
                    ? "Connect the projector to Wi-Fi first"
                    : "http://" + ip + ":" + port);
        } catch (IOException e) {
            address.setText("Couldn't start the upload page");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        server.stop();
    }

    /** Rebuilds the picture row and focuses {@code focus} (or the one in use). */
    private void refresh(File focus) {
        final int gen = ++generation;
        row.removeAllViews();
        File current = store.current();
        if (focus == null) focus = current;

        View none = tile(null, current == null);
        row.addView(none);
        View toFocus = none;

        final List<File> files = store.list();
        final ImageView[] thumbs = new ImageView[files.size()];
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            LinearLayout t = tile(f, f.equals(current));
            thumbs[i] = (ImageView) t.getChildAt(0);
            row.addView(t);
            if (f.equals(focus)) toFocus = t;
        }
        toFocus.requestFocus();

        final int w = Ui.dp(this, 240), h = Ui.dp(this, 135);
        new Thread(() -> {
            for (int i = 0; i < files.size(); i++) {
                if (gen != generation) return;
                final Bitmap b = WallpaperStore.load(files.get(i), w, h);
                final ImageView v = thumbs[i];
                runOnUiThread(() -> {
                    if (gen == generation) v.setImageBitmap(b);
                });
            }
        }, "thumbs").start();
    }

    private LinearLayout tile(final File f, boolean inUse) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setBackground(Ui.card(this, 12, 3));
        int p = Ui.dp(this, 6);
        t.setPadding(p, p, p, p);
        t.setFocusable(true);
        t.setClickable(true);

        int w = Ui.dp(this, 240), h = Ui.dp(this, 135);
        if (f == null) {
            TextView plain = new TextView(this);
            plain.setText("No picture");
            plain.setTextColor(Ui.TEXT_DIM);
            plain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            plain.setGravity(Gravity.CENTER);
            plain.setBackgroundColor(Ui.BG);
            t.addView(plain, new LinearLayout.LayoutParams(w, h));
        } else {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Ui.BG);
            t.addView(img, new LinearLayout.LayoutParams(w, h));
        }

        TextView caption = new TextView(this);
        caption.setText(inUse ? "In use" : " ");
        caption.setTextColor(Ui.TEXT);
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        caption.setPadding(0, Ui.dp(this, 6), 0, 0);
        t.addView(caption);

        t.setOnClickListener(v -> {
            store.setCurrent(f);
            refresh(f);
            Toast.makeText(this, f == null ? "Wallpaper removed" : "Wallpaper set", Toast.LENGTH_SHORT).show();
        });
        if (f != null) {
            t.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setMessage("Delete this picture?")
                        .setPositiveButton("Delete", (d, which) -> {
                            store.delete(f);
                            refresh(null);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, Ui.dp(this, 16), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView text(String s, float sp, int color, float topDp) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setPadding(0, Ui.dp(this, topDp), 0, 0);
        return v;
    }

    private byte[] readPage() {
        try (InputStream in = getResources().openRawResource(R.raw.upload)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}

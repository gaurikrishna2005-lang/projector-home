package home.projector;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps uploaded pictures (resized to at most 1920 px) and remembers which one is in use. */
final class WallpaperStore {
    private static final int MAX_EDGE = 1920;

    private final File dir;
    private final SharedPreferences prefs;

    WallpaperStore(Context c) {
        dir = new File(c.getFilesDir(), "wallpapers");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        prefs = c.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    File tempFile() {
        return new File(dir, "upload.tmp");
    }

    File current() {
        String name = prefs.getString("wallpaper", "");
        if (name.isEmpty()) return null;
        File f = new File(dir, name);
        return f.isFile() ? f : null;
    }

    void setCurrent(File f) {
        prefs.edit().putString("wallpaper", f == null ? "" : f.getName()).apply();
    }

    /** Newest first. */
    List<File> list() {
        List<File> out = new ArrayList<>();
        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) if (f.getName().endsWith(".jpg")) out.add(f);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    void delete(File f) {
        if (f.equals(current())) setCurrent(null);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    /** Turns an uploaded file into a stored wallpaper. Returns null if it isn't a picture. */
    File save(File raw) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(raw.getPath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap b = BitmapFactory.decodeFile(raw.getPath(), opts);
            if (b == null) return null;

            Matrix m = new Matrix();
            int edge = Math.max(b.getWidth(), b.getHeight());
            if (edge > MAX_EDGE) {
                float s = MAX_EDGE / (float) edge;
                m.postScale(s, s);
            }
            int degrees = rotation(raw);
            if (degrees != 0) m.postRotate(degrees);
            if (!m.isIdentity()) {
                Bitmap t = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (t != b) b.recycle();
                b = t;
            }

            File out = new File(dir, "wp_" + System.currentTimeMillis() + ".jpg");
            OutputStream os = new FileOutputStream(out);
            try {
                b.compress(Bitmap.CompressFormat.JPEG, 88, os);
            } finally {
                os.close();
                b.recycle();
            }
            return out;
        } catch (IOException | OutOfMemoryError e) {
            return null;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            raw.delete();
        }
    }

    /** Decodes just big enough for a w x h view, in a memory-light format. */
    static Bitmap load(File f, int w, int h) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            return BitmapFactory.decodeFile(f.getPath(), opts);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static int rotation(File f) {
        try {
            int o = new ExifInterface(f.getPath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}

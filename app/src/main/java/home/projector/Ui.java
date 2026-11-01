package home.projector;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Toast;

/** Shared colours and small helpers. */
final class Ui {
    static final int BG = 0xFF0E0E10;
    static final int CARD = 0xFF19191C;
    static final int CARD_FOCUS = 0xFF2B2B31;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xFF9A9AA0;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Dark card that gets a white outline when selected with the remote. */
    static Drawable card(Context c, float radiusDp, float strokeDp) {
        float r = dp(c, radiusDp);
        GradientDrawable on = new GradientDrawable();
        on.setColor(CARD_FOCUS);
        on.setCornerRadius(r);
        on.setStroke(dp(c, strokeDp), TEXT);

        GradientDrawable off = new GradientDrawable();
        off.setColor(CARD);
        off.setCornerRadius(r);

        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[]{android.R.attr.state_pressed}, on);
        s.addState(new int[]{android.R.attr.state_selected}, on);
        s.addState(new int[]{}, off);
        return s;
    }

    static ColorStateList focusText() {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{TEXT, TEXT_DIM});
    }

    static void launch(Activity a, Intent i, String name) {
        if (i == null) {
            Toast.makeText(a, name + " isn't installed", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            a.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(a, "Couldn't open " + name, Toast.LENGTH_SHORT).show();
        }
    }
}

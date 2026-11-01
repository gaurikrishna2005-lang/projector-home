package home.projector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.util.Locale;

/**
 * One thing on the home screen. It finds its app by package name first,
 * then by an exact component, then by searching app names for {@code match}.
 */
final class Tile {
    final String label;
    final String match;
    final String[] packages;
    ComponentName component;
    Intent action;

    Tile(String label, String match, String... packages) {
        this.label = label;
        this.match = match;
        this.packages = packages;
    }

    Tile withComponent(String pkg, String cls) {
        component = new ComponentName(pkg, cls);
        return this;
    }

    /** Opens a fixed intent instead of looking for an app (e.g. system Settings). */
    Tile withAction(Intent i) {
        action = i;
        return this;
    }

    Intent resolve(Context c) {
        PackageManager pm = c.getPackageManager();

        for (String p : packages) {
            Intent i = pm.getLeanbackLaunchIntentForPackage(p);
            if (i == null) i = pm.getLaunchIntentForPackage(p);
            if (i != null) return i;
        }

        if (component != null) {
            Intent i = new Intent(Intent.ACTION_MAIN).setComponent(component);
            if (pm.resolveActivity(i, 0) != null) return i;
        }

        if (match != null) {
            String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
            for (String cat : cats) {
                List<ResolveInfo> list = pm.queryIntentActivities(
                        new Intent(Intent.ACTION_MAIN).addCategory(cat), 0);
                for (ResolveInfo ri : list) {
                    if (ri.activityInfo.packageName.equals(c.getPackageName())) continue;
                    String name = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT);
                    if (name.contains(match)) {
                        return new Intent(Intent.ACTION_MAIN)
                                .addCategory(cat)
                                .setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    }
                }
            }
        }

        if (action != null && pm.resolveActivity(action, 0) != null) return new Intent(action);
        return null;
    }
}

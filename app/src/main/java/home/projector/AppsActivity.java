package home.projector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every installed app, so nothing is lost when the old home screen is switched off.
 * OK opens an app; holding OK opens its App info (to disable or uninstall it).
 */
public class AppsActivity extends Activity {

    private static final class App {
        String label, pkg, cls, category;
        Drawable icon;
    }

    private final List<App> apps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        loadApps();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int side = Ui.dp(this, 56);
        root.setPadding(side, Ui.dp(this, 32), side, 0);

        TextView title = new TextView(this);
        title.setText("All apps");
        title.setTextColor(Ui.TEXT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setPadding(0, 0, 0, Ui.dp(this, 20));
        root.addView(title);

        GridView grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setVerticalSpacing(Ui.dp(this, 12));
        grid.setHorizontalSpacing(Ui.dp(this, 12));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(Ui.card(this, 14, 3));
        grid.setDrawSelectorOnTop(false);
        grid.setClipToPadding(false);
        grid.setPadding(0, 0, 0, Ui.dp(this, 32));
        grid.setAdapter(new Adapter());
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                App a = apps.get(pos);
                Intent i = new Intent(Intent.ACTION_MAIN)
                        .addCategory(a.category)
                        .setClassName(a.pkg, a.cls);
                Ui.launch(AppsActivity.this, i, a.label);
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                App a = apps.get(pos);
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + a.pkg));
                Ui.launch(AppsActivity.this, i, "App info");
                return true;
            }
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        grid.requestFocus();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            for (ResolveInfo ri : pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                App a = new App();
                a.label = String.valueOf(ri.loadLabel(pm));
                a.pkg = pkg;
                a.cls = ri.activityInfo.name;
                a.category = cat;
                a.icon = ri.loadIcon(pm);
                apps.add(a);
            }
        }
        Collections.sort(apps, new Comparator<App>() {
            @Override public int compare(App x, App y) { return x.label.compareToIgnoreCase(y.label); }
        });
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int i) { return apps.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convert == null) {
                cell = new LinearLayout(AppsActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                int p = Ui.dp(AppsActivity.this, 12);
                cell.setPadding(p, p, p, p);

                icon = new ImageView(AppsActivity.this);
                int s = Ui.dp(AppsActivity.this, 48);
                cell.addView(icon, new LinearLayout.LayoutParams(s, s));

                label = new TextView(AppsActivity.this);
                label.setTextColor(Ui.TEXT);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setPadding(0, Ui.dp(AppsActivity.this, 8), 0, 0);
                cell.addView(label, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            } else {
                cell = (LinearLayout) convert;
            }
            icon = (ImageView) cell.getChildAt(0);
            label = (TextView) cell.getChildAt(1);
            App a = apps.get(pos);
            icon.setImageDrawable(a.icon);
            label.setText(a.label);
            return cell;
        }
    }
}

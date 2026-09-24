package dev.probecat.airplanescheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

// The app's whole design system: a few tokens and the components built from them.
final class Ui {
    static final int RADIUS = 12;
    static final int GAP = 8;
    static final int PAD = 16;
    static final int TEXT_SMALL = 13;
    static final int TEXT_BODY = 16;
    static final int TEXT_LARGE = 28;
    static final int TOUCH = 48;

    private final Context context;

    Ui(Context context) {
        this.context = context;
    }

    int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    int color(int resource) {
        return context.getColor(resource);
    }

    TextView text(String value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color(color));
        return text;
    }

    TextView label(String value) {
        TextView label = text(value, TEXT_SMALL, R.color.app_on_surface_muted);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    LinearLayout panel() {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(PAD), dp(GAP), dp(PAD), dp(GAP));
        panel.setBackground(box(R.color.app_surface, 0));
        return panel;
    }

    Button button(String value, boolean primary, View.OnClickListener onClick) {
        Button button = new Button(context);
        int content = primary ? R.color.app_on_primary : R.color.app_on_surface;
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(TEXT_BODY);
        button.setTextColor(color(content));
        button.setMinHeight(dp(TOUCH));
        button.setMinimumHeight(dp(TOUCH));
        button.setStateListAnimator(null);
        button.setBackground(pressable(primary ? box(R.color.app_primary, 0)
                : box(R.color.app_background, R.color.app_outline), content));
        button.setOnClickListener(onClick);
        return button;
    }

    // Looks like a value, acts like a button: tapping opens a picker.
    TextView field(View.OnClickListener onClick) {
        TextView field = text("", TEXT_LARGE, R.color.app_on_surface);
        field.setGravity(Gravity.CENTER);
        field.setMinHeight(dp(64));
        field.setBackground(pressable(box(R.color.app_background, R.color.app_outline), R.color.app_on_surface));
        field.setOnClickListener(onClick);
        return field;
    }

    TextView pill() {
        TextView pill = label("");
        pill.setPadding(dp(10), dp(4), dp(10), dp(4));
        return pill;
    }

    void setPill(TextView pill, String value, String description, boolean good) {
        pill.setText(value);
        pill.setContentDescription(description);
        pill.setTextColor(color(good ? R.color.status_ready_foreground : R.color.status_off_foreground));
        pill.setBackground(box(good ? R.color.status_ready_background : R.color.status_off_background, 0));
    }

    // A short label, an info button for the long description, and a switch; the whole row toggles.
    Toggle toggle(LinearLayout parent, String value, String description, boolean checked) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(TOUCH));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(text(value, TEXT_BODY, R.color.app_on_surface));

        ImageButton info = new ImageButton(context);
        info.setImageResource(R.drawable.ic_info);
        info.setImageTintList(ColorStateList.valueOf(color(R.color.app_on_surface_muted)));
        info.setBackground(pressable(null, R.color.app_on_surface));
        info.setContentDescription("About " + value);
        info.setTooltipText(description);
        info.setOnClickListener(view -> new AlertDialog.Builder(context)
                .setTitle(value)
                .setMessage(description)
                .setPositiveButton("OK", null)
                .show());
        row.addView(info, new LinearLayout.LayoutParams(dp(40), dp(40)));

        row.addView(new View(context), new LinearLayout.LayoutParams(0, 0, 1));

        Toggle toggle = new Toggle(context);
        toggle.setContentDescription(value);
        toggle.setChecked(checked);
        row.addView(toggle);
        row.setOnClickListener(view -> toggle.toggle());
        return toggle;
    }

    private GradientDrawable box(int fill, int stroke) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(color(fill));
        box.setCornerRadius(dp(RADIUS));
        if (stroke != 0) {
            box.setStroke(dp(1), color(stroke));
        }
        return box;
    }

    private RippleDrawable pressable(Drawable background, int content) {
        int ripple = (color(content) & 0x00ffffff) | 0x33000000;
        return new RippleDrawable(ColorStateList.valueOf(ripple), background, null);
    }
}

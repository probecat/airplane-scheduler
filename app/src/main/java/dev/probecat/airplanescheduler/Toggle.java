package dev.probecat.airplanescheduler;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.CompoundButton;
import android.widget.Switch;

// A 52x32dp on/off switch. Position and colors move together in 100ms, unlike the framework
// Switch's fixed 250ms slide with an instant color change.
final class Toggle extends CompoundButton {
    private static final ArgbEvaluator COLORS = new ArgbEvaluator();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final int trackOn;
    private final int trackOff;
    private final int thumbOn;
    private final int thumbOff;
    private final ValueAnimator animator;
    private float position;

    Toggle(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        trackOn = context.getColor(R.color.app_primary);
        trackOff = context.getColor(R.color.app_outline);
        thumbOn = context.getColor(R.color.app_on_primary);
        thumbOff = context.getColor(R.color.app_surface);
        animator = ValueAnimator.ofFloat().setDuration(100);
        animator.addUpdateListener(animation -> {
            position = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void setChecked(boolean checked) {
        boolean changed = checked != isChecked();
        super.setChecked(checked);
        float target = checked ? 1 : 0;
        // The superclass constructor calls this before the animator exists.
        if (animator == null || !changed || !isLaidOut()) {
            position = target;
            invalidate();
            return;
        }
        animator.cancel();
        animator.setFloatValues(position, target);
        animator.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(52 * density), Math.round(32 * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float height = getHeight();
        paint.setColor((int) COLORS.evaluate(position, trackOff, trackOn));
        canvas.drawRoundRect(0, 0, getWidth(), height, height / 2, height / 2, paint);
        paint.setColor((int) COLORS.evaluate(position, thumbOff, thumbOn));
        float radius = 10 * density;
        float inset = height / 2;
        float x = inset + (getWidth() - 2 * inset) * position;
        canvas.drawCircle(x, height / 2, radius, paint);
    }

    // Announced as a switch by accessibility services.
    @Override
    public CharSequence getAccessibilityClassName() {
        return Switch.class.getName();
    }
}

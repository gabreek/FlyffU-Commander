package medev.gabreek.flyffucommander;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class FabMovementHandler {

    private int screenWidth;
    private int screenHeight;
    private final Handler longPressHandler = new Handler();

    public FabMovementHandler(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupFabTouchListener(View fab, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        final Runnable longPressRunnable = fab::performLongClick;
        fab.setOnTouchListener((v, e) -> {
            final int X = (int) e.getRawX();
            final int Y = (int) e.getRawY();

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setTag(new float[]{v.getX() - X, v.getY() - Y, e.getRawX(), e.getRawY(), e.getDownTime()});
                    longPressHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_UP:
                    longPressHandler.removeCallbacks(longPressRunnable);
                    float[] tagUp = (float[]) v.getTag();
                    long dur = (long) (e.getEventTime() - tagUp[4]);
                    float dx = Math.abs(e.getRawX() - tagUp[2]);
                    float dy = Math.abs(e.getRawY() - tagUp[3]);
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if (dx < slop && dy < slop && dur < ViewConfiguration.getLongPressTimeout()) {
                        v.performClick();
                    } else {
                        snapFabToEdge(v);
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float[] tagMove = (float[]) v.getTag();
                    if (Math.abs(e.getRawX() - tagMove[2]) > ViewConfiguration.get(v.getContext()).getScaledTouchSlop()
                            || Math.abs(e.getRawY() - tagMove[3]) > ViewConfiguration.get(v.getContext()).getScaledTouchSlop()) {
                        longPressHandler.removeCallbacks(longPressRunnable);
                    }
                    v.setX(X + tagMove[0]);
                    v.setY(Y + tagMove[1]);
                    return true;
            }
            return false;
        });
        fab.setOnClickListener(clickListener);
        fab.setOnLongClickListener(longClickListener);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupDraggableFab(View view, FabPositionSaver positionSaver) {
        view.setOnTouchListener((v, event) -> {
            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    v.setTag(new float[]{v.getX() - X, v.getY() - Y, event.getRawX(), event.getRawY(), event.getDownTime()});
                    return true;
                case MotionEvent.ACTION_UP:
                    float[] tagUp = (float[]) v.getTag();
                    long eventDuration = (long) (event.getEventTime() - tagUp[4]);
                    float dx = Math.abs(event.getRawX() - tagUp[2]);
                    float dy = Math.abs(event.getRawY() - tagUp[3]);
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if (dx < slop && dy < slop && eventDuration < ViewConfiguration.getLongPressTimeout()) {
                        v.performClick();
                    } else {
                        snapFabToEdge(v);
                        if (positionSaver != null) {
                            positionSaver.saveFabHideShowPosition(v.getX(), v.getY());
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float[] tagMove = (float[]) v.getTag();
                    v.setX(X + tagMove[0]);
                    v.setY(Y + tagMove[1]);
                    return true;
            }
            return false;
        });
    }

    public void snapFabToEdge(View v) {
        int w = v.getWidth();
        int h = v.getHeight();
        float x = v.getX();
        float y = v.getY();

        if (x < -w / 2f) x = -w / 2f;
        if (x > screenWidth - w / 2f) x = screenWidth - w / 2f;

        if (y < -h / 2f) y = -h / 2f;
        if (y > screenHeight - h / 2f) y = screenHeight - h / 2f;

        ObjectAnimator.ofFloat(v, "x", x).setDuration(200).start();
        ObjectAnimator.ofFloat(v, "y", y).setDuration(200).start();
    }

    public interface FabPositionSaver {
        void saveFabHideShowPosition(float x, float y);
    }
}
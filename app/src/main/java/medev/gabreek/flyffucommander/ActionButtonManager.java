package medev.gabreek.flyffucommander;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActionButtonManager {

    private final Context context;
    private final FrameLayout rootContainer;
    private final Map<Integer, List<ActionButtonData>> clientActionButtonsData;
    private final Map<View, ActionButtonData> fabViewToActionDataMap;
    private final TinyDB appTinyDB;
    private final DisplayUtils displayUtils;
    private final KeyDispatcher keyDispatcher;
    private boolean areActionButtonsPositionsFixed;
    private final Function<Integer, String> clientDisplayNameProvider;
    private final Supplier<SparseArray<WebView>> webViewsProvider;
    private final FabMovementHandler fabMovementHandler;

    public ActionButtonManager(Context context, FrameLayout rootContainer, Map<Integer, List<ActionButtonData>> clientActionButtonsData,
                               Map<View, ActionButtonData> fabViewToActionDataMap, TinyDB appTinyDB, DisplayUtils displayUtils,
                               KeyDispatcher keyDispatcher, boolean areActionButtonsPositionsFixed,
                               Function<Integer, String> clientDisplayNameProvider, Supplier<SparseArray<WebView>> webViewsProvider) {
        this.context = context;
        this.rootContainer = rootContainer;
        this.clientActionButtonsData = clientActionButtonsData;
        this.fabViewToActionDataMap = fabViewToActionDataMap;
        this.appTinyDB = appTinyDB;
        this.displayUtils = displayUtils;
        this.keyDispatcher = keyDispatcher;
        this.areActionButtonsPositionsFixed = areActionButtonsPositionsFixed;
        this.clientDisplayNameProvider = clientDisplayNameProvider;
        this.webViewsProvider = webViewsProvider;
        this.fabMovementHandler = new FabMovementHandler(((MainActivity) context).getScreenWidth(), ((MainActivity) context).getScreenHeight());
    }

    public void setActionButtonsPositionsFixed(boolean fixed) {
        this.areActionButtonsPositionsFixed = fixed;
    }

    public FrameLayout createCustomFab(ActionButtonData buttonData) {
        FrameLayout fabContainer = new FrameLayout(context);
        int fabSizePx = displayUtils.dpToPx(40);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(fabSizePx, fabSizePx);
        containerParams.leftMargin = (int) buttonData.x;
        containerParams.topMargin = (int) buttonData.y;
        fabContainer.setLayoutParams(containerParams);
        fabContainer.setAlpha(0.5f);

        android.graphics.drawable.GradientDrawable circularBackground = new android.graphics.drawable.GradientDrawable();
        circularBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        if (buttonData.macroType == ActionButtonData.TYPE_TIMED_REPEAT_MACRO && buttonData.isToggleOn) {
            circularBackground.setColor(Color.CYAN);
        } else {
            circularBackground.setColor(buttonData.color);
        }
        fabContainer.setBackground(circularBackground);
        fabContainer.setElevation(0f);

        TextView label = new TextView(context);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.CENTER;
        label.setLayoutParams(labelParams);
        label.setText(buttonData.keyText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setClickable(false);
        label.setFocusable(false);

        fabContainer.addView(label);

        fabContainer.setOnClickListener(v -> {
            WebView targetWebView = webViewsProvider.get().get(buttonData.clientId);
            if (targetWebView != null) {
                keyDispatcher.dispatchKeyEvent(targetWebView, buttonData);
            } else {
                Toast.makeText(context, "Client " + clientDisplayNameProvider.apply(buttonData.clientId) + " is not active.", Toast.LENGTH_SHORT).show();
            }
        });

        rootContainer.addView(fabContainer);
        fabViewToActionDataMap.put(fabContainer, buttonData);

        List<ActionButtonData> clientSpecificButtons = clientActionButtonsData.computeIfAbsent(buttonData.clientId, k -> new ArrayList<>());

        boolean buttonExists = false;
        for (int i = 0; i < clientSpecificButtons.size(); i++) {
            if (clientSpecificButtons.get(i).keyText.equals(buttonData.keyText)) {
                clientSpecificButtons.set(i, buttonData);
                buttonExists = true;
                break;
            }
        }
        if (!buttonExists) {
            clientSpecificButtons.add(buttonData);
        }

        if (!areActionButtonsPositionsFixed) {
            makeFabDraggable(fabContainer);
        }

        return fabContainer;
    }

    private void makeFabDraggable(View view) {
        view.setOnTouchListener((v, event) -> {
            if (areActionButtonsPositionsFixed) return false;

            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();

            switch (event.getAction() & android.view.MotionEvent.ACTION_MASK) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setTag(new float[]{v.getX() - X, v.getY() - Y, event.getRawX(), event.getRawY(), event.getDownTime()});
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    float[] tagUp = (float[]) v.getTag();
                    long eventDuration = (long) (event.getEventTime() - tagUp[4]);
                    float dx = Math.abs(event.getRawX() - tagUp[2]);
                    float dy = Math.abs(event.getRawY() - tagUp[3]);
                    int slop = ViewConfiguration.get(context).getScaledTouchSlop();

                    if (dx < slop && dy < slop && eventDuration < ViewConfiguration.getLongPressTimeout()) {
                        v.performClick();
                    } else {
                        fabMovementHandler.snapFabToEdge(v);
                        ActionButtonData data = fabViewToActionDataMap.get(v);
                        if (data != null) {
                            data.x = v.getX();
                            data.y = v.getY();
                            saveActionButtonsState(data.clientId);
                        }
                    }
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    float[] tagMove = (float[]) v.getTag();
                    v.setX(X + tagMove[0]);
                    v.setY(Y + tagMove[1]);
                    return true;
            }
            return false;
        });
    }

    public void refreshAllActionButtonsDisplay(boolean isVisible, FloatingActionButton fabHideShow, int activeClientId) {
        deleteAllCustomFabs();

        boolean hasAnyActionButtons = clientActionButtonsData.values().stream().anyMatch(list -> !list.isEmpty());

        for (Map.Entry<Integer, List<ActionButtonData>> entry : clientActionButtonsData.entrySet()) {
            for (ActionButtonData data : entry.getValue()) {
                createCustomFab(data);
            }
        }

        if (fabHideShow != null) {
            if (hasAnyActionButtons) {
                fabHideShow.setVisibility(View.VISIBLE);
                fabHideShow.setImageResource(isVisible ? R.drawable.ic_hide_show : R.drawable.ic_show_hide);
            } else {
                fabHideShow.setVisibility(View.GONE);
            }
        }

        for (View fab : fabViewToActionDataMap.keySet()) {
            fab.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }

    public void deleteAllCustomFabs() {
        for (View fab : fabViewToActionDataMap.keySet()) {
            rootContainer.removeView(fab);
        }
        fabViewToActionDataMap.clear();
    }

    public void saveActionButtonsState(int clientId) {
        List<ActionButtonData> dataToSave = clientActionButtonsData.get(clientId);
        if (dataToSave == null) {
            dataToSave = new ArrayList<>();
        }
        String json = new Gson().toJson(dataToSave);
        TinyDB tinyDB = new TinyDB(context, "client_prefs_" + clientId);
        tinyDB.putString(Constants.ACTION_BUTTONS_DATA_KEY, json);
    }

    public void loadActionButtonsState(int clientId) {
        TinyDB tinyDB = new TinyDB(context, "client_prefs_" + clientId);
        String json = tinyDB.getString(Constants.ACTION_BUTTONS_DATA_KEY);

        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<List<ActionButtonData>>() {}.getType();
            List<ActionButtonData> loadedData = new Gson().fromJson(json, type);
            if (loadedData != null) {
                // Set all toggles to off when loading
                for (ActionButtonData data : loadedData) {
                    data.isToggleOn = false;
                }
                clientActionButtonsData.put(clientId, loadedData);
            }
        }
        if (clientActionButtonsData.get(clientId) == null) {
            clientActionButtonsData.put(clientId, new ArrayList<>());
        }
    }

    public void updateActionButtonToggleState(ActionButtonData buttonData) {
        List<ActionButtonData> clientButtons = clientActionButtonsData.get(buttonData.clientId);
        if (clientButtons != null) {
            for (int i = 0; i < clientButtons.size(); i++) {
                ActionButtonData existingData = clientButtons.get(i);
                if (existingData.keyText.equals(buttonData.keyText) && existingData.clientId == buttonData.clientId) {
                    existingData.isToggleOn = buttonData.isToggleOn; // Update the state
                    saveActionButtonsState(buttonData.clientId); // Save immediately
                    break;
                }
            }
        }

        // Update button appearance to reflect toggle state
        View fabView = null;
        for (Map.Entry<View, ActionButtonData> entry : fabViewToActionDataMap.entrySet()) {
            if (entry.getValue().keyText.equals(buttonData.keyText) && entry.getValue().clientId == buttonData.clientId) {
                fabView = entry.getKey();
                break;
            }
        }

        if (fabView != null) {
            android.graphics.drawable.GradientDrawable background = (android.graphics.drawable.GradientDrawable) fabView.getBackground();
            if (background != null) {
                if (buttonData.isToggleOn) {
                    background.setColor(Color.CYAN);
                } else {
                    background.setColor(buttonData.color); // Revert to original color
                }
            }
        }
    }
}
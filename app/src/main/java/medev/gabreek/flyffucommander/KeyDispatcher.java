package medev.gabreek.flyffucommander;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyDispatcher {

    private final Context context;
    private final Map<String, Handler> timedRepeatMacroHandlers = new HashMap<>();
    private final Map<Integer, List<ActionButtonData>> clientActionButtonsData;
    private final Map<View, ActionButtonData> fabViewToActionDataMap;
    private final SparseArray<WebView> webViews;
    private ActionButtonManager actionButtonManager;


    public KeyDispatcher(Context context, SparseArray<WebView> webViews, Map<Integer, List<ActionButtonData>> clientActionButtonsData, Map<View, ActionButtonData> fabViewToActionDataMap) {
        this.context = context;
        this.webViews = webViews;
        this.clientActionButtonsData = clientActionButtonsData;
        this.fabViewToActionDataMap = fabViewToActionDataMap;
    }

    public void setActionButtonManager(ActionButtonManager actionButtonManager) {
        this.actionButtonManager = actionButtonManager;
    }

    public void dispatchKeyEvent(WebView webView, ActionButtonData buttonData) {
        switch (buttonData.macroType) {
            case ActionButtonData.TYPE_NORMAL:
                dispatchSingleKeyEvent(webView, buttonData.keyCode, buttonData.isAltPressed, buttonData.isCtrlPressed);
                break;
            case ActionButtonData.TYPE_MACRO:
                executeMacro(webView, buttonData);
                break;
            case ActionButtonData.TYPE_TIMED_REPEAT_MACRO:
                toggleTimedRepeatMacro(webView, buttonData);
                break;
            case ActionButtonData.TYPE_COMBO:
                executeCombo(webView, buttonData);
                break;
        }
    }

    private void dispatchSingleKeyEvent(WebView webView, int keyCode, boolean isAltPressed, boolean isCtrlPressed) {
        String key;
        String code;
        int jsKeyCode;
        switch (keyCode) {
            case KeyEvent.KEYCODE_F1: key = "F1"; code = "F1"; jsKeyCode = 112; break;
            case KeyEvent.KEYCODE_F2: key = "F2"; code = "F2"; jsKeyCode = 113; break;
            case KeyEvent.KEYCODE_F3: key = "F3"; code = "F3"; jsKeyCode = 114; break;
            case KeyEvent.KEYCODE_F4: key = "F4"; code = "F4"; jsKeyCode = 115; break;
            case KeyEvent.KEYCODE_F5: key = "F5"; code = "F5"; jsKeyCode = 116; break;
            case KeyEvent.KEYCODE_F6: key = "F6"; code = "F6"; jsKeyCode = 117; break;
            case KeyEvent.KEYCODE_F7: key = "F7"; code = "F7"; jsKeyCode = 118; break;
            case KeyEvent.KEYCODE_F8: key = "F8"; code = "F8"; jsKeyCode = 119; break;
            case KeyEvent.KEYCODE_F9: key = "F9"; code = "F9"; jsKeyCode = 120; break;
            case KeyEvent.KEYCODE_F10: key = "F10"; code = "F10"; jsKeyCode = 121; break;
            case KeyEvent.KEYCODE_F11: key = "F11"; code = "F11"; jsKeyCode = 122; break;
            case KeyEvent.KEYCODE_F12: key = "F12"; code = "F12"; jsKeyCode = 123; break;
            case KeyEvent.KEYCODE_NUMPAD_0: key = "0"; code = "Numpad0"; jsKeyCode = 48; break;
            case KeyEvent.KEYCODE_NUMPAD_1: key = "1"; code = "Numpad1"; jsKeyCode = 49; break;
            case KeyEvent.KEYCODE_NUMPAD_2: key = "2"; code = "Numpad2"; jsKeyCode = 50; break;
            case KeyEvent.KEYCODE_NUMPAD_3: key = "3"; code = "Numpad3"; jsKeyCode = 51; break;
            case KeyEvent.KEYCODE_NUMPAD_4: key = "4"; code = "Numpad4"; jsKeyCode = 52; break;
            case KeyEvent.KEYCODE_NUMPAD_5: key = "5"; code = "Numpad5"; jsKeyCode = 53; break;
            case KeyEvent.KEYCODE_NUMPAD_6: key = "6"; code = "Numpad6"; jsKeyCode = 54; break;
            case KeyEvent.KEYCODE_NUMPAD_7: key = "7"; code = "Numpad7"; jsKeyCode = 55; break;
            case KeyEvent.KEYCODE_NUMPAD_8: key = "8"; code = "Numpad8"; jsKeyCode = 56; break;
            case KeyEvent.KEYCODE_NUMPAD_9: key = "9"; code = "Numpad9"; jsKeyCode = 57; break;
            case KeyEvent.KEYCODE_0: key = "0"; code = "Digit0"; jsKeyCode = 48; break;
            case KeyEvent.KEYCODE_1: key = "1"; code = "Digit1"; jsKeyCode = 49; break;
            case KeyEvent.KEYCODE_2: key = "2"; code = "Digit2"; jsKeyCode = 50; break;
            case KeyEvent.KEYCODE_3: key = "3"; code = "Digit3"; jsKeyCode = 51; break;
            case KeyEvent.KEYCODE_4: key = "4"; code = "Digit4"; jsKeyCode = 52; break;
            case KeyEvent.KEYCODE_5: key = "5"; code = "Digit5"; jsKeyCode = 53; break;
            case KeyEvent.KEYCODE_6: key = "6"; code = "Digit6"; jsKeyCode = 54; break;
            case KeyEvent.KEYCODE_7: key = "7"; code = "Digit7"; jsKeyCode = 55; break;
            case KeyEvent.KEYCODE_8: key = "8"; code = "Digit8"; jsKeyCode = 56; break;
            case KeyEvent.KEYCODE_9: key = "9"; code = "Digit9"; jsKeyCode = 57; break;
            default:
                key = String.valueOf((char) (new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)).getUnicodeChar());
                code = "Key" + key.toUpperCase();
                jsKeyCode = keyCode; // Fallback to Android keyCode for others
                break;
        }

        String script = "javascript:(function() {" +
                "var canvas = document.querySelector('canvas');" +
                "if (canvas) {" ;

        if (isAltPressed) {
            script += "   canvas.dispatchEvent(new KeyboardEvent('keydown', { key: 'Alt', code: 'AltLeft', keyCode: 18, bubbles: true, cancelable: true }));";
        }
        if (isCtrlPressed) {
            script += "   canvas.dispatchEvent(new KeyboardEvent('keydown', { key: 'Control', code: 'ControlLeft', keyCode: 17, bubbles: true, cancelable: true }));";
        }

        script += "   var mainEventProps = { bubbles: true, cancelable: true, key: '" + key + "', code: '" + code + "', keyCode: " + jsKeyCode + " };" +
                  "   canvas.dispatchEvent(new KeyboardEvent('keydown', mainEventProps));" +
                  "   canvas.dispatchEvent(new KeyboardEvent('keyup', mainEventProps));";

        if (isAltPressed) {
            script += "   canvas.dispatchEvent(new KeyboardEvent('keyup', { key: 'Alt', code: 'AltLeft', keyCode: 18, bubbles: true, cancelable: true }));";
        }
        if (isCtrlPressed) {
            script += "   canvas.dispatchEvent(new KeyboardEvent('keyup', { key: 'Control', code: 'ControlLeft', keyCode: 17, bubbles: true, cancelable: true }));";
        }

        script += "}" +
                  "})()";
        webView.evaluateJavascript(script, null);
    }

    private void executeMacro(WebView webView, ActionButtonData buttonData) {
        String[] keys = buttonData.macroKeys.split(",");
        Handler handler = new Handler();
        for (int i = 0; i < keys.length; i++) {
            final int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + keys[i].trim().toUpperCase());
            final int delay = (int) (buttonData.delayBetweenKeys * 1000 * i);
            handler.postDelayed(() -> dispatchSingleKeyEvent(webView, keyCode, buttonData.isAltPressed, buttonData.isCtrlPressed), delay);
        }
    }

    private void executeCombo(WebView webView, ActionButtonData buttonData) {
        Handler handler = new Handler();
        long delay = 0;

        // Dispatch Main Function Key
        handler.postDelayed(() -> dispatchSingleKeyEvent(webView, buttonData.comboMainKey, false, false), delay);
        delay += 100; // Small delay between keys

        // Dispatch Digit Key
        handler.postDelayed(() -> dispatchSingleKeyEvent(webView, buttonData.comboDigitKey, false, false), delay);
        delay += 100; // Small delay between keys

        // Dispatch After Pressed Key
        handler.postDelayed(() -> dispatchSingleKeyEvent(webView, buttonData.comboAfterPressedKey, false, false), delay);
    }

    private void toggleTimedRepeatMacro(WebView webView, ActionButtonData buttonData) {
        buttonData.isToggleOn = !buttonData.isToggleOn;
        actionButtonManager.updateActionButtonToggleState(buttonData);

        if (buttonData.isToggleOn) {
            // Start repeating
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    dispatchSingleKeyEvent(webView, buttonData.repeatKey, buttonData.isAltPressed, buttonData.isCtrlPressed);
                    handler.postDelayed(this, (long) (buttonData.repeatInterval * 1000));
                }
            };
            timedRepeatMacroHandlers.put(buttonData.keyText, handler);
            // Initial dispatch
            dispatchSingleKeyEvent(webView, buttonData.repeatKey, buttonData.isAltPressed, buttonData.isCtrlPressed);
            handler.postDelayed(runnable, (long) (buttonData.repeatInterval * 1000));
        } else {
            // Stop repeating
            if (timedRepeatMacroHandlers.containsKey(buttonData.keyText)) {
                timedRepeatMacroHandlers.get(buttonData.keyText).removeCallbacksAndMessages(null);
                timedRepeatMacroHandlers.remove(buttonData.keyText);
            }
        }

        
    }

    public void stopAllTimedRepeatMacros() {
        for (Handler handler : timedRepeatMacroHandlers.values()) {
            handler.removeCallbacksAndMessages(null);
        }
        timedRepeatMacroHandlers.clear();
    }
}
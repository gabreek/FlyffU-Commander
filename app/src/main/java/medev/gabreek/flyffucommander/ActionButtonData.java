package medev.gabreek.flyffucommander;

import java.util.Objects;

public class ActionButtonData {
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MACRO = 1;
    public static final int TYPE_TIMED_REPEAT_MACRO = 2;
    public static final int TYPE_COMBO = 3; // New type for combo button

    String keyText;
    int keyCode;
    float x;
    float y;
    int color; // Store color as an int
    int clientId; // Add clientId field
    int macroType; // 0: normal, 1: macro, 2: timed repeat macro, 3: combo
    String macroKeys; // Comma-separated key codes for macro
    float delayBetweenKeys; // Delay in seconds for macro
    int repeatKey; // Single key code for timed repeat macro
    float repeatInterval; // Interval in seconds for timed repeat macro
    boolean isToggleOn; // For timed repeat macro
    boolean isAltPressed; // New field for Alt modifier
    boolean isCtrlPressed; // New field for Ctrl modifier
    int comboMainKey; // New field for combo button's main function key
    int comboDigitKey; // New field for combo button's digit key
    int comboAfterPressedKey; // New field for combo button's after pressed function key

    public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId) {
        this(keyText, keyCode, x, y, color, clientId, TYPE_NORMAL, null, 0.0f, 0, 0.0f, false, false, false, 0, 0, 0);
    }

    public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId, int macroType, String macroKeys, float delayBetweenKeys, int repeatKey, float repeatInterval, boolean isToggleOn) {
        this(keyText, keyCode, x, y, color, clientId, macroType, macroKeys, delayBetweenKeys, repeatKey, repeatInterval, isToggleOn, false, false, 0, 0, 0);
    }

    public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId, int macroType, String macroKeys, float delayBetweenKeys, int repeatKey, float repeatInterval, boolean isToggleOn, boolean isAltPressed, boolean isCtrlPressed) {
        this(keyText, keyCode, x, y, color, clientId, macroType, macroKeys, delayBetweenKeys, repeatKey, repeatInterval, isToggleOn, isAltPressed, isCtrlPressed, 0, 0, 0);
    }

    public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId, int macroType, String macroKeys, float delayBetweenKeys, int repeatKey, float repeatInterval, boolean isToggleOn, boolean isAltPressed, boolean isCtrlPressed, int comboMainKey, int comboDigitKey, int comboAfterPressedKey) {
        this.keyText = keyText;
        this.keyCode = keyCode;
        this.x = x;
        this.y = y;
        this.color = color;
        this.clientId = clientId;
        this.macroType = macroType;
        this.macroKeys = macroKeys;
        this.delayBetweenKeys = delayBetweenKeys;
        this.repeatKey = repeatKey;
        this.repeatInterval = repeatInterval;
        this.isToggleOn = isToggleOn;
        this.isAltPressed = isAltPressed;
        this.isCtrlPressed = isCtrlPressed;
        this.comboMainKey = comboMainKey;
        this.comboDigitKey = comboDigitKey;
        this.comboAfterPressedKey = comboAfterPressedKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionButtonData that = (ActionButtonData) o;
        return keyCode == that.keyCode &&
               Float.compare(that.x, x) == 0 &&
               Float.compare(that.y, y) == 0 &&
               color == that.color &&
               clientId == that.clientId &&
               macroType == that.macroType &&
               Float.compare(that.delayBetweenKeys, delayBetweenKeys) == 0 &&
               repeatKey == that.repeatKey &&
               Float.compare(that.repeatInterval, repeatInterval) == 0 &&
               isToggleOn == that.isToggleOn &&
               isAltPressed == that.isAltPressed &&
               isCtrlPressed == that.isCtrlPressed &&
               comboMainKey == that.comboMainKey &&
               comboDigitKey == that.comboDigitKey &&
               comboAfterPressedKey == that.comboAfterPressedKey &&
               keyText.equals(that.keyText) &&
               Objects.equals(macroKeys, that.macroKeys);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(keyText, keyCode, x, y, color, clientId, macroType, macroKeys, delayBetweenKeys, repeatKey, repeatInterval, isToggleOn, isAltPressed, isCtrlPressed, comboMainKey, comboDigitKey, comboAfterPressedKey);
    }
}

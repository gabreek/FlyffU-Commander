package medev.gabreek.flyffucommander;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    private final Context mContext;
    public WebAppInterface(Context c) { mContext = c; }
    @JavascriptInterface public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}

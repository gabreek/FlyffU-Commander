package medev.gabreek.flyffucommander;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class LocalStorageInterface {
    private final TinyDB db;
    public LocalStorageInterface(Context context, int clientId) {
        this.db = new TinyDB(context, "client_prefs_" + clientId);
    }
    @JavascriptInterface public void setItem(String k, String v) { db.putString(k, v); }
    @JavascriptInterface public String getItem(String k) { return db.getString(k); }
    @JavascriptInterface public void removeItem(String k) { db.remove(k); }
    @JavascriptInterface public void clear() { db.clear(); }
}

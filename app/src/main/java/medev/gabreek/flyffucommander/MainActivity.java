package medev.gabreek.flyffucommander;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.stream.Stream;
import android.text.InputFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int WIKI_CLIENT_ID = -4;
    private static final int MADRIGAL_CLIENT_ID = -2;
    private static final int FLYFFULATOR_CLIENT_ID = -3;

    private static final String WIKI_URL = "https://flyff-wiki.gpotato.com.br/wiki/Main_Page";
    private static final String MADRIGAL_URL = "https://madrigalinside.com/";
    private static final String FLYFFULATOR_URL = "https://flyffulator.com/";

    private static final int MAX_CLIENTS = 10;
    private static final String CLIENT_NAME_KEY = "client_custom_name";
    private static final String ACTION_BUTTONS_DATA_KEY = "action_buttons_data";

    private final SparseArray<WebView> webViews = new SparseArray<>();
    private final SparseArray<FrameLayout> layouts = new SparseArray<>();
    private int activeClientId = -1;
    private TinyDB appTinyDB;
    private final Set<Integer> configuredClientIds = new HashSet<>();

    private LinearLayout linearLayout;
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton fabHideShow;
    private boolean isActionButtonsVisible = true;
    private FrameLayout rootContainer;
    private final Map<Integer, List<ActionButtonData>> clientActionButtonsData = new HashMap<>();
    private final Map<View, ActionButtonData> fabViewToActionDataMap = new HashMap<>();
    private final Map<String, Integer> keyCodeMap = new HashMap<>();

    private Gson gson = new Gson();

    private String userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.5304.105 Mobile Safari/537.36";
    private String url = "https://universe.flyff.com/play";
    private boolean exit = false;

    // Class to hold action button data for serialization
    private static class ActionButtonData {
        public static final int TYPE_NORMAL = 0;
        public static final int TYPE_MACRO = 1;
        public static final int TYPE_TIMED_REPEAT_MACRO = 2;

        String keyText;
        int keyCode;
        float x;
        float y;
        int color; // Store color as an int
        int clientId; // Add clientId field
        int macroType; // 0: normal, 1: macro, 2: timed repeat macro
        String macroKeys; // Comma-separated key codes for macro
        float delayBetweenKeys; // Delay in seconds for macro
        int repeatKey; // Single key code for timed repeat macro
        float repeatInterval; // Interval in seconds for timed repeat macro
        boolean isToggleOn; // For timed repeat macro

        public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId) {
            this(keyText, keyCode, x, y, color, clientId, TYPE_NORMAL, null, 0.0f, 0, 0.0f, false);
        }

        public ActionButtonData(String keyText, int keyCode, float x, float y, int color, int clientId, int macroType, String macroKeys, float delayBetweenKeys, int repeatKey, float repeatInterval, boolean isToggleOn) {
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
                   keyText.equals(that.keyText) &&
                   Objects.equals(macroKeys, that.macroKeys);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(keyText, keyCode, x, y, color, clientId, macroType, macroKeys, delayBetweenKeys, repeatKey, repeatInterval, isToggleOn);
        }
    }

    /* FAB movement */
    private int _xDelta;
    private int _yDelta;
    private int screenWidth;
    private int screenHeight;
    private long downTime;
    private float initialRawX;
    private float initialRawY;
    private final Handler longPressHandler = new Handler();
    private Runnable longPressRunnable;

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /* JS
 Android bridges */
    public static class LocalStorageInterface {
        private final TinyDB db;
        LocalStorageInterface(Context context, int clientId) {
            this.db = new TinyDB(context, "client_prefs_" + clientId);
        }
        @JavascriptInterface public void setItem(String k, String v) { db.putString(k, v); }
        @JavascriptInterface public String getItem(String k) { return db.getString(k); }
        @JavascriptInterface public void removeItem(String k) { db.remove(k); }
        @JavascriptInterface public void clear() { db.clear(); }
    }

    public static class WebAppInterface {
        private final Context mContext;
        WebAppInterface(Context c) { mContext = c; }
        @JavascriptInterface public void showKeyboard() {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appTinyDB = new TinyDB(this, "app_prefs");
        isActionButtonsVisible = appTinyDB.getBoolean("isActionButtonsVisible");
        setContentView(R.layout.activity_main);
        setTitle("FlyffU Android");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        fullScreenOn();

        rootContainer = findViewById(R.id.root_container);
        linearLayout   = findViewById(R.id.linearLayout);
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setAlpha(0.5f);

        fabHideShow = findViewById(R.id.fab_hide_show);
        fabHideShow.setOnClickListener(v -> {
            isActionButtonsVisible = !isActionButtonsVisible;
            appTinyDB.putBoolean("isActionButtonsVisible", isActionButtonsVisible);
            refreshAllActionButtonsDisplay();
        });

        float fabHideShowX = appTinyDB.getFloat("fabHideShow_x");
        float fabHideShowY = appTinyDB.getFloat("fabHideShow_y");
        fabHideShow.setX(fabHideShowX);
        fabHideShow.setY(fabHideShowY);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth  = dm.widthPixels;
        screenHeight = dm.heightPixels;

        initializeKeyCodeMap();
        setupFabTouchListener(floatingActionButton);
        setupHideShowFabTouchListener(fabHideShow);

        fabHideShow.setAlpha(0.5f);

        List<Integer> storedClientIds = appTinyDB.getListInt("configuredClientIds");
        if (storedClientIds != null && !storedClientIds.isEmpty()) {
            configuredClientIds.addAll(storedClientIds);
        } else {
            configuredClientIds.addAll(getExistingClientIdsFromFileSystem());
        }

        // Load all action buttons for all configured clients at startup
        for (int clientId : configuredClientIds) {
            loadActionButtonsState(clientId);
        }

        if (savedInstanceState == null) {
            if (configuredClientIds.isEmpty()) {
                createNewClient();
            } else {
                int first = Collections.min(configuredClientIds);
                if (webViews.get(first) == null) openClient(first);
                else switchToClient(first);
            }
        }
        refreshAllActionButtonsDisplay();
    }

    /* ---------- WebView creation with .bin caching ---------- */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void createWebViewer(WebView webView, FrameLayout frameLayout, int clientId, String initialUrl) {
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webView.addJavascriptInterface(new LocalStorageInterface(this, clientId), "AndroidLocalStorage");
        webView.addJavascriptInterface(new WebAppInterface(this), "FlyffUAndroid");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                /* localStorage override */
                view.evaluateJavascript(
                        "(function(){"
                        + "window.localStorage.setItem=function(k,v){AndroidLocalStorage.setItem(k,v)};"
                        + "window.localStorage.getItem=function(k){return AndroidLocalStorage.getItem(k)};"
                        + "window.localStorage.removeItem=function(k){AndroidLocalStorage.removeItem(k)};"
                        + "window.localStorage.clear=function(){AndroidLocalStorage.clear()};"
                        + "})()", null);

                /* .bin caching via IndexedDB */
                view.evaluateJavascript(
                        "(function(){\n"
                        + "const BIN=/\\.bin$/i,IDB_NAME='flyff_bin_cache',STORE='blobs',VER=1;\n"
                        + "let db;\n"
                        + "const openDb=()=>new Promise((res,rej)=>{ \n"
                        + "  const r=indexedDB.open(IDB_NAME,VER);\n"
                        + "  r.onupgradeneeded=()=>r.result.createObjectStore(STORE);\n"
                        + "  r.onsuccess=()=>res(r.result);\n"
                        + "  r.onerror=()=>rej(r.error);\n"
                        + "});\n"
                        + "const key=u=>{try{return new URL(u).origin+new URL(u).pathname}catch{return u.split('')[0]}};\n"
                        + "const get=u=>openDb().then(d=>d.transaction(STORE,'readonly').objectStore(STORE).get(key(u)));\n"
                        + "const put=(u,b)=>openDb().then(d=>d.transaction(STORE,'readwrite').objectStore(STORE).put(b,key(u)));\n"
                        + "const Native=XMLHttpRequest;\n"
                        + "window.XMLHttpRequest=function(){\n"
                        + "  const xhr=new Native,open=xhr.open,send=xhr.send;\n"
                        + "  xhr.open=function(m,u,...a){\n"
                        + "    this._url=u;this._bin=BIN.test(u);\n"
                        + "    return open.call(this,m,u,...a);\n"
                        + "  };\n"
                        + "  xhr.send=function(...a){\n"
                        + "    if(!this._bin)return send.apply(this,a);\n"
                        + "    const u=this._url;\n"
                        + "    get(u).then(blob=>{\n"
                        + "      if(blob){\n"
                        + "        ['response','responseText','readyState','status','statusText'].forEach(p=>Object.defineProperty(xhr,p,{writable:true}));\n"
                        + "        xhr.response=blob;xhr.responseText='';xhr.readyState=4;xhr.status=200;xhr.statusText='OK';\n"
                        + "        if(xhr.onreadystatechange)xhr.onreadystatechange();\n"
                        + "        if(xhr.onload)xhr.onload();\n"
                        + "        return;\n"
                        + "      }\n"
                        + "      xhr.addEventListener('load',()=>{\n"
                        + "        if(xhr.status===200&&xhr.response instanceof Blob)put(u,xhr.response);\n"
                        + "      });\n"
                        + "      send.apply(xhr,a);\n"
                        + "    });\n"
                        + "  };\n"
                        + "  return xhr;\n"
                        + "};\n"
                        + "})()", null);
            }
        });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAppCacheEnabled(true);
        s.setAppCachePath(getCacheDir().getAbsolutePath());
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(userAgent);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        frameLayout.addView(webView);
        webView.requestFocus();
        webView.loadUrl(initialUrl);
    }

    /* ---------- rest of the file unchanged ---------- */

    @SuppressLint("ClickableViewAccessibility")
    private void setupFabTouchListener(View fab) {
        longPressRunnable = () -> floatingActionButton.performLongClick();
        floatingActionButton.setOnTouchListener((v, e) -> {
            int X = (int) e.getRawX(), Y = (int) e.getRawY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downTime = e.getDownTime();
                    initialRawX = e.getRawX();
                    initialRawY = e.getRawY();
                    _xDelta = X - (int) v.getX();
                    _yDelta = Y - (int) v.getY();
                    longPressHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_UP:
                    longPressHandler.removeCallbacks(longPressRunnable);
                    long dur = e.getEventTime() - downTime;
                    float dx = Math.abs(e.getRawX() - initialRawX);
                    float dy = Math.abs(e.getRawY() - initialRawY);
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if (dx < slop && dy < slop && dur < ViewConfiguration.getLongPressTimeout())
                        v.performClick();
                    else snapFabToEdge(v);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getRawX() - initialRawX) > ViewConfiguration.get(v.getContext()).getScaledTouchSlop()
                            || Math.abs(e.getRawY() - initialRawY) > ViewConfiguration.get(v.getContext()).getScaledTouchSlop())
                        longPressHandler.removeCallbacks(longPressRunnable);
                    v.setX(X - _xDelta);
                    v.setY(Y - _yDelta);
                    return true;
            }
            return false;
        });
        floatingActionButton.setOnClickListener(v -> switchToNextClient());
        floatingActionButton.setOnLongClickListener(v -> {
            showClientManagerMenu(v);
            return true;
        });
    }

    private void snapFabToEdge(View v) {
        int w = v.getWidth();
        float x = v.getX();
        if (x < -w / 2f) x = -w / 2f;
        if (x > screenWidth - w / 2f) x = screenWidth - w / 2f;
        ObjectAnimator.ofFloat(v, "x", x).setDuration(200).start();
    }

    /* ---------- client helper methods ---------- */

    private void openClient(int id) {
        if (webViews.get(id) != null) { switchToClient(id); return; }
        if (webViews.size() >= MAX_CLIENTS) {
            Toast.makeText(this, "Max clients open", Toast.LENGTH_SHORT).show();
            return;
        }
        FrameLayout fl = new FrameLayout(this);
        fl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(fl);
        layouts.put(id, fl);

        WebView w = new CustomWebView(getApplicationContext());
        createWebViewer(w, fl, id, url);
        webViews.put(id, w);
        switchToClient(id);
        floatingActionButton.setVisibility(View.VISIBLE);
        refreshAllActionButtonsDisplay();
    }

    private void switchToClient(int id) {
        if (layouts.get(id) == null) {
            if (webViews.size() > 0) switchToClient(webViews.keyAt(0));
            else {
                activeClientId = -1;
                setTitle("FlyffU Android");
                floatingActionButton.setVisibility(View.GONE);
            }
            return;
        }
        // Save state of previously active client before switching
        if (activeClientId != -1 && activeClientId != id) {
            saveActionButtonsState(activeClientId);
        }

        for (int i = 0; i < layouts.size(); i++) {
            int k = layouts.keyAt(i);
            layouts.get(k).setVisibility(k == id ? View.VISIBLE : View.GONE);
        }
        activeClientId = id;
        loadClientState(activeClientId);
        loadActionButtonsState(activeClientId);
        setTitle(getClientDisplayName(id));
        WebView w = webViews.get(id);
        if (w != null) {
            w.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(w.getWindowToken(), 0);
        }

    }

    private void switchToNextClient() {
        if (webViews.size() > 1) {
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < webViews.size(); i++) ids.add(webViews.keyAt(i));
            Collections.sort(ids);
            int idx = (ids.indexOf(activeClientId) + 1) % ids.size();
            switchToClient(ids.get(idx));
        }
    }

    private void killClient(int id) {
        if (webViews.get(id) == null) return;
        WebView w = webViews.get(id);
        FrameLayout f = layouts.get(id);
        f.removeAllViews();
        w.destroy();
        linearLayout.removeView(f);
        webViews.remove(id);
        layouts.remove(id);
        Toast.makeText(this, getClientDisplayName(id) + " killed", Toast.LENGTH_SHORT).show();
        if (webViews.size() == 0) {
            activeClientId = -1;
            setTitle("FlyffU Android");
            floatingActionButton.setVisibility(View.GONE);
        } else if (activeClientId == id) {
            activeClientId = webViews.keyAt(0);
            switchToClient(activeClientId);
        }
        refreshAllActionButtonsDisplay();
    }

    private void deleteClient(int id) {
        if (webViews.get(id) != null) killClient(id);
        configuredClientIds.remove(id);
        appTinyDB.putListInt("configuredClientIds", new ArrayList<>(configuredClientIds));
        TinyDB db = new TinyDB(this, "client_prefs_" + id);
        db.clear();
        File f = new File(getApplicationInfo().dataDir, "shared_prefs/client_prefs_" + id + ".xml");
        if (f.exists()) f.delete();
        Toast.makeText(this, "Client " + id + " deleted", Toast.LENGTH_SHORT).show();
        if (configuredClientIds.isEmpty()) createNewClient();
    }

    private List<Integer> getExistingClientIdsFromFileSystem() {
        List<Integer> ids = new ArrayList<>();
        File dir = new File(getApplicationInfo().dataDir, "shared_prefs");
        if (dir.exists() && dir.isDirectory()) {
            Pattern p = Pattern.compile("client_prefs_(\\d+)\\.xml");
            File[] fs = dir.listFiles();
            if (fs != null)
                for (File f : fs) {
                    Matcher m = p.matcher(f.getName());
                    if (m.matches()) try {
                        ids.add(Integer.parseInt(m.group(1)));
                    } catch (NumberFormatException ignore) {}
                }
        }
        return ids;
    }

    private String getClientDisplayName(int id) {
        if (id == WIKI_CLIENT_ID) return "Flyff Wiki";
        if (id == MADRIGAL_CLIENT_ID) return "Madrigal Inside";
        if (id == FLYFFULATOR_CLIENT_ID) return "Flyffulator";
        TinyDB db = new TinyDB(this, "client_prefs_" + id);
        String custom = db.getString(CLIENT_NAME_KEY);
        return (custom != null && !custom.isEmpty()) ? custom : "Client " + id;
    }

    /* ---------- FAB long-press menu ---------- */
    private void showClientManagerMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "New Client");

        List<Integer> sorted = new ArrayList<>(configuredClientIds);
        Collections.sort(sorted);
        if (!sorted.isEmpty()) {
            SubMenu sub = popup.getMenu().addSubMenu(Menu.NONE, 2, Menu.NONE, "Clients");
            for (int id : sorted) {
                SubMenu sm = sub.addSubMenu(getClientDisplayName(id));
                boolean open = webViews.get(id) != null;
                if (open) {
                    sm.add(Menu.NONE, 6000 + id, 0, "Action Buttons"); // Moved to first position with order 0
                    sm.add(Menu.NONE, 1000 + id, 1, "Switch");
                    sm.add(Menu.NONE, 2000 + id, 2, "Kill");
                } else sm.add(Menu.NONE, 3000 + id, 1, "Open");
                sm.add(Menu.NONE, 4000 + id, 3, "Rename");
                sm.add(Menu.NONE, 5000 + id, 4, "Delete");

            }
        }
        SubMenu util = popup.getMenu().addSubMenu(Menu.NONE, 3, Menu.NONE, "Utils");
        util.add(Menu.NONE, 7000 + Math.abs(WIKI_CLIENT_ID), Menu.NONE, "Flyff Wiki");
        util.add(Menu.NONE, 7000 + Math.abs(MADRIGAL_CLIENT_ID), Menu.NONE, "Madrigal Inside");
        util.add(Menu.NONE, 7000 + Math.abs(FLYFFULATOR_CLIENT_ID), Menu.NONE, "Flyffulator");

        popup.setOnMenuItemClickListener(item -> {
            int id = -1, itemId = item.getItemId();
            if (itemId > 1000) {
                if (itemId < 2000) id = itemId - 1000;
                else if (itemId < 3000) id = itemId - 2000;
                else if (itemId < 4000) id = itemId - 3000;
                else if (itemId < 5000) id = itemId - 4000;
                else if (itemId < 6000) id = itemId - 5000;
                else if (itemId < 7000) id = itemId - 6000;
                else if (itemId < 8000) id = -(itemId - 7000);
            }
            if (itemId == 1) createNewClient();
            else if (id != -1) {
                if (itemId >= 1000 && itemId < 2000) switchToClient(id);
                else if (itemId >= 2000 && itemId < 3000) confirmKillClient(id);
                else if (itemId >= 3000 && itemId < 4000) openClient(id);
                else if (itemId >= 4000 && itemId < 5000) showRenameDialog(id);
                else if (itemId >= 5000 && itemId < 6000) confirmDeleteClient(id);
                else if (itemId >= 6000 && itemId < 7000) {
                    if (webViews.get(id) == null) {
                        openClient(id);
                    } else {
                        switchToClient(id);
                    }
                    showActionButtonsMenu();
                }
                else if (itemId >= 7000 && itemId < 8000) openUtilityClient(id);
            }
            return true;
        });
        popup.show();
    }

    private void createNewClient() {
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < webViews.size(); i++) open.add(webViews.keyAt(i));
        for (int id : configuredClientIds) {
            if (!open.contains(id)) {
                if (webViews.size() >= MAX_CLIENTS) {
                    Toast.makeText(this, "Max open clients reached", Toast.LENGTH_SHORT).show();
                    return;
                }
                openClient(id);
                Toast.makeText(this, getClientDisplayName(id) + " opened", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (webViews.size() >= MAX_CLIENTS) {
            Toast.makeText(this, "Max clients reached", Toast.LENGTH_SHORT).show();
            return;
        }
        int newId = 1;
        while (configuredClientIds.contains(newId) && newId <= MAX_CLIENTS) newId++;
        if (newId > MAX_CLIENTS) {
            Toast.makeText(this, "No free slot", Toast.LENGTH_SHORT).show();
            return;
        }
        configuredClientIds.add(newId);
        openClient(newId);
        Toast.makeText(this, "Client " + newId + " created", Toast.LENGTH_SHORT).show();
    }

    private void openUtilityClient(int id) {
        String u;
        switch (id) {
            case WIKI_CLIENT_ID: u = WIKI_URL; break;
            case MADRIGAL_CLIENT_ID: u = MADRIGAL_URL; break;
            case FLYFFULATOR_CLIENT_ID: u = FLYFFULATOR_URL; break;
            default: return;
        }
        if (webViews.get(id) != null) {
            if (activeClientId == id) confirmCloseUtilityClient(id);
            else switchToClient(id);
            return;
        }
        if (webViews.size() >= MAX_CLIENTS) {
            Toast.makeText(this, "Max clients reached", Toast.LENGTH_SHORT).show();
            return;
        }
        FrameLayout fl = new FrameLayout(this);
        fl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(fl);
        layouts.put(id, fl);

        WebView w = new CustomWebView(getApplicationContext());
        createWebViewer(w, fl, id, u);
        webViews.put(id, w);
        switchToClient(id);
        floatingActionButton.setVisibility(View.VISIBLE);
        Toast.makeText(this, getClientDisplayName(id) + " opened", Toast.LENGTH_SHORT).show();
    }

    private void confirmKillClient(int id) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Kill?")
                .setMessage("Kill " + getClientDisplayName(id) + "?")
                .setPositiveButton("Yes", (d, w) -> killClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmDeleteClient(int id) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Delete?")
                .setMessage("Delete " + getClientDisplayName(id) + "? This is permanent.")
                .setPositiveButton("Yes", (d, w) -> deleteClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmCloseUtilityClient(int id) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Close?")
                .setMessage("Close " + getClientDisplayName(id) + "?")
                .setPositiveButton("Yes", (d, w) -> closeUtilityClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    private void closeUtilityClient(int id) {
        if (webViews.get(id) == null) return;
        WebView w = webViews.get(id);
        FrameLayout f = layouts.get(id);
        f.removeAllViews();
        w.destroy();
        linearLayout.removeView(f);
        webViews.remove(id);
        layouts.remove(id);
        Toast.makeText(this, getClientDisplayName(id) + " closed", Toast.LENGTH_SHORT).show();
        if (webViews.size() == 0) {
            activeClientId = -1;
            setTitle("FlyffU Android");
            floatingActionButton.setVisibility(View.GONE);
        } else {
            activeClientId = webViews.keyAt(0);
            switchToClient(activeClientId);
        }
    }

    private void showRenameDialog(int id) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Rename Client");
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(getClientDisplayName(id));
        b.setView(input);
        b.setPositiveButton("Save", (d, w) -> {
            String n = input.getText().toString().trim();
            if (!n.isEmpty()) {
                TinyDB db = new TinyDB(this, "client_prefs_" + id);
                db.putString(CLIENT_NAME_KEY, n);
                Toast.makeText(this, "Renamed to " + n, Toast.LENGTH_SHORT).show();
                if (activeClientId == id) setTitle(n);
            } else Toast.makeText(this, "Empty name", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    /* ---------- Action Buttons methods ---------- */

    private void initializeKeyCodeMap() {
        for (int i = 0; i < 12; i++) {
            keyCodeMap.put("F" + (i + 1), KeyEvent.KEYCODE_F1 + i);
        }
        // Add other keys as needed
        keyCodeMap.put("A", KeyEvent.KEYCODE_A);
        keyCodeMap.put("B", KeyEvent.KEYCODE_B);
        // ... and so on
    }

    private void showActionButtonsMenu() {
        final CharSequence[] items = {"New", "Color", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Action Button Configuration");
        builder.setItems(items, (dialog, item) -> {
            String selectedOption = items[item].toString();
            if (selectedOption.equals("New")) {
                showKeyTypeDialog();
            } else if (selectedOption.equals("Color")) {
                showColorSelectionDialog(activeClientId);
            } else if (selectedOption.equals("Delete")) {
                showDeleteActionButtonDialog(activeClientId);
            }
        });
        builder.show();
    }

    private void showKeyTypeDialog() {
        final CharSequence[] items = {"Function Key", "Custom Key", "Macro", "Timed Repeat Macro"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Key Type");
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Function Key")) {
                showFunctionKeyDialog();
            } else if (items[item].equals("Custom Key")) {
                showCustomKeyDialog();
            } else if (items[item].equals("Macro")) {
                showMacroCreationDialog();
            } else if (items[item].equals("Timed Repeat Macro")) {
                showTimedRepeatMacroCreationDialog();
            }
        });
        builder.show();
    }

    private void showColorSelectionDialog(int clientId) {
        List<ActionButtonData> clientButtons = clientActionButtonsData.get(clientId);
        if (clientButtons == null || clientButtons.isEmpty()) {
            Toast.makeText(this, "No action buttons for this client to color.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] buttonLabels = Stream.concat(Stream.of("All buttons"), clientButtons.stream()
                .map(data -> data.keyText))
                .toArray(CharSequence[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Button to Color");
        builder.setItems(buttonLabels, (dialog, whichButton) -> {
            String selectedKeyText = buttonLabels[whichButton].toString();
            if (selectedKeyText.equals("All buttons")) {
                final CharSequence[] colorNames = {"Red", "Green", "Blue", "Black", "White", "Gray"};
                final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.WHITE, Color.GRAY};

                AlertDialog.Builder colorBuilder = new AlertDialog.Builder(this);
                colorBuilder.setTitle("Select Color for All Buttons");
                colorBuilder.setItems(colorNames, (colorDialog, whichColor) -> {
                    int newColor = colors[whichColor];
                    for (ActionButtonData data : clientButtons) {
                        data.color = newColor;
                        // Find the corresponding FAB view and update its color
                        for (Map.Entry<View, ActionButtonData> entry : fabViewToActionDataMap.entrySet()) {
                            if (entry.getValue().keyText.equals(data.keyText) && entry.getValue().clientId == data.clientId) {
                                android.graphics.drawable.GradientDrawable background = (android.graphics.drawable.GradientDrawable) entry.getKey().getBackground();
                                if (background != null) {
                                    background.setColor(newColor);
                                }
                                break;
                            }
                        }
                    }
                    Toast.makeText(this, "All buttons colored " + colorNames[whichColor], Toast.LENGTH_SHORT).show();
                    saveActionButtonsState(clientId); // Save state after color change
                });
                colorBuilder.show();
            } else {
                // The user selected a specific button. The index in the dialog is `whichButton`.
                // Since "All buttons" is at index 0, the index in our `clientButtons` list is `whichButton - 1`.
                int buttonIndex = whichButton - 1;
                if (buttonIndex < 0 || buttonIndex >= clientButtons.size()) {
                    // Should not happen, but as a safeguard.
                    return;
                }
                
                final ActionButtonData selectedButtonData = clientButtons.get(buttonIndex);

                // Find the corresponding view to update its color live.
                View selectedFabView = null;
                for (Map.Entry<View, ActionButtonData> entry : fabViewToActionDataMap.entrySet()) {
                    if (entry.getValue() == selectedButtonData) { // Compare by object reference
                        selectedFabView = entry.getKey();
                        break;
                    }
                }

                if (selectedFabView != null) {
                    final View finalSelectedFabView = selectedFabView;
                    final CharSequence[] colorNames = {"Red", "Green", "Blue", "Black", "White", "Gray"};
                    final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.WHITE, Color.GRAY};

                    AlertDialog.Builder colorBuilder = new AlertDialog.Builder(this);
                    colorBuilder.setTitle("Select Color for " + selectedButtonData.keyText);
                    colorBuilder.setItems(colorNames, (colorDialog, whichColor) -> {
                        int newColor = colors[whichColor];
                        selectedButtonData.color = newColor;

                        // Update the background color of the existing FAB view
                        android.graphics.drawable.GradientDrawable background = (android.graphics.drawable.GradientDrawable) finalSelectedFabView.getBackground();
                        if (background != null) {
                            background.setColor(newColor);
                        }
                        Toast.makeText(this, selectedButtonData.keyText + " color changed to " + colorNames[whichColor], Toast.LENGTH_SHORT).show();
                        saveActionButtonsState(clientId); // Save state after color change
                    });
                    colorBuilder.show();
                }
            }
        });
        builder.show();
    }

    private void showDeleteActionButtonDialog(int clientId) {
        List<ActionButtonData> clientButtons = clientActionButtonsData.get(clientId);
        if (clientButtons == null || clientButtons.isEmpty()) {
            Toast.makeText(this, "No action buttons for this client to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] buttonLabels = clientButtons.stream()
                .map(data -> data.keyText)
                .toArray(CharSequence[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Button to Delete");
        builder.setItems(buttonLabels, (dialog, whichButton) -> {
            String selectedKeyText = buttonLabels[whichButton].toString();
            
            // Find the ActionButtonData object and its corresponding View
            ActionButtonData dataToRemove = null;
            for (ActionButtonData data : clientButtons) {
                if (data.keyText.equals(selectedKeyText)) {
                    dataToRemove = data;
                    break;
                }
            }

            if (dataToRemove != null) {
                // Find the view associated with the data to remove it from the layout
                View fabViewToRemove = null;
                for (Map.Entry<View, ActionButtonData> entry : fabViewToActionDataMap.entrySet()) {
                    if (entry.getValue().equals(dataToRemove)) {
                        fabViewToRemove = entry.getKey();
                        break;
                    }
                }

                if (fabViewToRemove != null) {
                    rootContainer.removeView(fabViewToRemove);
                    fabViewToActionDataMap.remove(fabViewToRemove);
                }

                clientButtons.remove(dataToRemove);
                saveActionButtonsState(clientId);
                refreshAllActionButtonsDisplay();
                Toast.makeText(this, selectedKeyText + " Action Button deleted.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showFunctionKeyDialog() {
        final CharSequence[] fKeys = new CharSequence[12];
        for (int i = 0; i < 12; i++) {
            fKeys[i] = "F" + (i + 1);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Function Key");
        builder.setItems(fKeys, (dialog, item) -> {
            String key = fKeys[item].toString();
            // Default position (0,0) and black color
            ActionButtonData newButtonData = new ActionButtonData(key, (int)keyCodeMap.get(key), 0f, 0f, Color.BLACK, activeClientId);
            createCustomFab(newButtonData);
            saveActionButtonsState(activeClientId);
            fabHideShow.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Action Button for '" + newButtonData.keyText + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showCustomKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Custom Key");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String key = input.getText().toString().toUpperCase();
            if (key.length() == 1) {
                int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key);
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    // Default position (0,0) and black color
                    ActionButtonData newButtonData = new ActionButtonData(key, keyCode, 0f, 0f, Color.BLACK, activeClientId);
                    createCustomFab(newButtonData);
                    saveActionButtonsState(activeClientId);
                    fabHideShow.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Action Button for '" + newButtonData.keyText + "' created.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a single character", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showMacroCreationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Macro Button");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Macro Name (max 2 letters)");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2)});
        layout.addView(nameInput);

        final EditText keysInput = new EditText(this);
        keysInput.setHint("Keys (e.g., 1,2,3)");
        keysInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(keysInput);

        TextView delayLabel = new TextView(this);
        delayLabel.setText("Delay between keys: 0.5s");
        layout.addView(delayLabel);

        SeekBar delaySlider = new SeekBar(this);
        delaySlider.setMax(45); // 0.5s to 5s, step 0.1s (45 steps)
        delaySlider.setProgress(0); // Start at 0.5s
        delaySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float delay = 0.5f + progress * 0.1f;
                delayLabel.setText("Delay between keys: " + new DecimalFormat("0.0").format(delay) + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(delaySlider);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().toUpperCase();
            String keys = keysInput.getText().toString();
            float delay = 0.5f + delaySlider.getProgress() * 0.1f;

            if (name.isEmpty() || keys.isEmpty()) {
                Toast.makeText(this, "Name and keys cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.length() > 2) {
                Toast.makeText(this, "Name must be max 2 letters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Default position (0,0) and black color
            ActionButtonData newButtonData = new ActionButtonData(name, 0, 0f, 0f, Color.BLACK, activeClientId, ActionButtonData.TYPE_MACRO, keys, delay, 0, 0.0f, false);
            createCustomFab(newButtonData);
            saveActionButtonsState(activeClientId);
            fabHideShow.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Macro Button '" + name + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showTimedRepeatMacroCreationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Timed Repeat Macro Button");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Macro Name (max 2 letters)");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2)});
        layout.addView(nameInput);

        final EditText keyInput = new EditText(this);
        keyInput.setHint("Single Key (e.g., 1)");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT);
        keyInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});
        layout.addView(keyInput);

        TextView intervalLabel = new TextView(this);
        intervalLabel.setText("Repeat Interval: 0.5s");
        layout.addView(intervalLabel);

        SeekBar intervalSlider = new SeekBar(this);
        intervalSlider.setMax(195); // 0.5s to 20s, step 0.1s (195 steps)
        intervalSlider.setProgress(0); // Start at 0.5s
        intervalSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float interval = 0.5f + progress * 0.1f;
                intervalLabel.setText("Repeat Interval: " + new DecimalFormat("0.0").format(interval) + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(intervalSlider);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().toUpperCase();
            String key = keyInput.getText().toString();
            float interval = 0.5f + intervalSlider.getProgress() * 0.1f;

            if (name.isEmpty() || key.isEmpty()) {
                Toast.makeText(this, "Name and key cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.length() > 2) {
                Toast.makeText(this, "Name must be max 2 letters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (key.length() != 1) {
                Toast.makeText(this, "Key must be a single character", Toast.LENGTH_SHORT).show();
                return;
            }

            int repeatKeyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key.toUpperCase());
            if (repeatKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
                Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
                return;
            }

            // Default position (0,0) and black color
            ActionButtonData newButtonData = new ActionButtonData(name, 0, 0f, 0f, Color.BLACK, activeClientId, ActionButtonData.TYPE_TIMED_REPEAT_MACRO, null, 0.0f, repeatKeyCode, interval, false);
            createCustomFab(newButtonData);
            saveActionButtonsState(activeClientId);
            fabHideShow.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Timed Repeat Macro Button '" + name + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private FrameLayout createCustomFab(ActionButtonData buttonData) {
        // Create a FrameLayout to hold the FAB and the TextView
        FrameLayout fabContainer = new FrameLayout(this);
        int fabSizePx = dpToPx(40); // Adjusted size for action buttons
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(fabSizePx, fabSizePx);
        // Set initial position from buttonData
        containerParams.leftMargin = (int) buttonData.x;
        containerParams.topMargin = (int) buttonData.y;
        fabContainer.setLayoutParams(containerParams);
        fabContainer.setAlpha(0.5f); // Set transparency

        // Programmatically create circular background with specified color
        android.graphics.drawable.GradientDrawable circularBackground = new android.graphics.drawable.GradientDrawable();
        circularBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        if (buttonData.macroType == ActionButtonData.TYPE_TIMED_REPEAT_MACRO && buttonData.isToggleOn) {
            circularBackground.setColor(Color.CYAN);
        } else {
            circularBackground.setColor(buttonData.color); // Use color from buttonData
        }
        fabContainer.setBackground(circularBackground); // Set circular background directly on container
        fabContainer.setElevation(0f); // Remove shadow

        // Create the TextView for the label
        TextView label = new TextView(this);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.CENTER; // Center the text within the button
        label.setLayoutParams(labelParams);
        label.setText(buttonData.keyText); // Use keyText from buttonData
        label.setTextColor(Color.WHITE);
        label.setTextSize(12); // Reduced text size for better fit
        label.setClickable(false); // Ensure TextView does not consume clicks
        label.setFocusable(false); // Ensure TextView does not consume focus

        // Add TextView to the container (only child)
        fabContainer.addView(label);

        // Set the click listener on the container
        fabContainer.setOnClickListener(v -> {
            WebView targetWebView = webViews.get(buttonData.clientId);
            if (targetWebView != null) {
                dispatchKeyEvent(targetWebView, buttonData);
            } else {
                Toast.makeText(this, "Client " + getClientDisplayName(buttonData.clientId) + " is not active.", Toast.LENGTH_SHORT).show();
            }
        });

        // Add the container to the root view and the map
        rootContainer.addView(fabContainer);
        fabViewToActionDataMap.put(fabContainer, buttonData);

        // Ensure clientActionButtonsData is updated for this button's client
        List<ActionButtonData> clientSpecificButtons = clientActionButtonsData.get(buttonData.clientId);
        if (clientSpecificButtons == null) {
            clientSpecificButtons = new ArrayList<>();
            clientActionButtonsData.put(buttonData.clientId, clientSpecificButtons);
        }

        // Check if this button (by keyText and keyCode) already exists in the client's list
        // If it exists, update its data. Otherwise, add it.
        boolean buttonExistsInClientList = false;
        for (int i = 0; i < clientSpecificButtons.size(); i++) {
            ActionButtonData existingData = clientSpecificButtons.get(i);
            if (existingData.keyText.equals(buttonData.keyText) && existingData.keyCode == buttonData.keyCode) {
                clientSpecificButtons.set(i, buttonData); // Update existing data
                buttonExistsInClientList = true;
                break;
            }
        }
        if (!buttonExistsInClientList) {
            clientSpecificButtons.add(buttonData); // Add new button data
        }

        // Make the entire container draggable
        makeFabDraggable(fabContainer);

        return fabContainer;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void makeFabDraggable(View view) {
        final float[] xDelta = new float[1];
        final float[] yDelta = new float[1];
        final float[] initialRawX = new float[1];
        final float[] initialRawY = new float[1];
        final long[] downTime = new long[1];

        view.setOnTouchListener((v, event) -> {
            int X = (int) event.getRawX();
            int Y = (int) event.getRawY();
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    downTime[0] = event.getDownTime();
                    initialRawX[0] = event.getRawX();
                    initialRawY[0] = event.getRawY();
                    xDelta[0] = X - v.getX();
                    yDelta[0] = Y - v.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    long eventDuration = event.getEventTime() - downTime[0];
                    float dx = Math.abs(event.getRawX() - initialRawX[0]);
                    float dy = Math.abs(event.getRawY() - initialRawY[0]);
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if (dx < slop && dy < slop && eventDuration < ViewConfiguration.getLongPressTimeout()) {
                        v.performClick();
                    } else {
                        snapFabToEdge(v);
                        // Update ActionButtonData and save state
                        ActionButtonData data = fabViewToActionDataMap.get(v);
                        if (data != null) {
                            data.x = v.getX();
                            data.y = v.getY();
                            saveActionButtonsState(data.clientId);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    v.setX(X - xDelta[0]);
                    v.setY(Y - yDelta[0]);
                    return true;
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupHideShowFabTouchListener(View view) {
        final float[] xDelta = new float[1];
        final float[] yDelta = new float[1];
        final float[] initialRawX = new float[1];
        final float[] initialRawY = new float[1];
        final long[] downTime = new long[1];

        view.setOnTouchListener((v, event) -> {
            int X = (int) event.getRawX();
            int Y = (int) event.getRawY();
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    downTime[0] = event.getDownTime();
                    initialRawX[0] = event.getRawX();
                    initialRawY[0] = event.getRawY();
                    xDelta[0] = X - v.getX();
                    yDelta[0] = Y - v.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    long eventDuration = event.getEventTime() - downTime[0];
                    float dx = Math.abs(event.getRawX() - initialRawX[0]);
                    float dy = Math.abs(event.getRawY() - initialRawY[0]);
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if (dx < slop && dy < slop && eventDuration < ViewConfiguration.getLongPressTimeout()) {
                        v.performClick();
                    } else {
                        snapFabToEdge(v);
                        appTinyDB.putFloat("fabHideShow_x", v.getX());
                        appTinyDB.putFloat("fabHideShow_y", v.getY());
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    v.setX(X - xDelta[0]);
                    v.setY(Y - yDelta[0]);
                    return true;
            }
            return false;
        });
    }


    private void refreshAllActionButtonsDisplay() {
        deleteAllCustomFabs(); // Clear all existing FABs

        boolean hasAnyActionButtons = false;
        for (Map.Entry<Integer, List<ActionButtonData>> entry : clientActionButtonsData.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                hasAnyActionButtons = true;
                break; // Found at least one action button
            }
        }

        if (hasAnyActionButtons) {
            fabHideShow.setVisibility(View.VISIBLE);
            fabHideShow.setImageResource(isActionButtonsVisible ? R.drawable.ic_hide_show : R.drawable.ic_show_hide);
        } else {
            fabHideShow.setVisibility(View.GONE);
        }

        // Now, display only the action buttons for active clients if isActionButtonsVisible is true
        if (isActionButtonsVisible) {
            for (Map.Entry<Integer, List<ActionButtonData>> entry : clientActionButtonsData.entrySet()) {
                int clientId = entry.getKey();
                if (webViews.get(clientId) != null) { // Only display buttons for active clients
                    for (ActionButtonData data : entry.getValue()) {
                        createCustomFab(data);
                    }
                }
            }
        }
    }

    private void deleteAllCustomFabs() {
        for (View fab : fabViewToActionDataMap.keySet()) {
            rootContainer.removeView(fab);
        }
        fabViewToActionDataMap.clear();


    }

    private void saveActionButtonsState(int clientId) {
        List<ActionButtonData> dataToSave = clientActionButtonsData.get(clientId);
        if (dataToSave == null) {
            dataToSave = new ArrayList<>(); // Should not happen if loadActionButtonsState is called, but for safety
        }
        String json = gson.toJson(dataToSave);
        TinyDB tinyDB = new TinyDB(this, "client_prefs_" + clientId);
        tinyDB.putString(ACTION_BUTTONS_DATA_KEY, json);
    }

    private void loadActionButtonsState(int clientId) {
        TinyDB tinyDB = new TinyDB(this, "client_prefs_" + clientId);
        String json = tinyDB.getString(ACTION_BUTTONS_DATA_KEY);

        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<List<ActionButtonData>>() {}.getType();
            List<ActionButtonData> loadedData = gson.fromJson(json, type);
            if (loadedData != null) {
                clientActionButtonsData.put(clientId, loadedData);
            }
        }
        if (clientActionButtonsData.get(clientId) == null) {
            clientActionButtonsData.put(clientId, new ArrayList<>());
        }
    }

    private void saveClientState(int clientId) {
        TinyDB tinyDB = new TinyDB(this, "client_prefs_" + clientId);
        WebView webView = webViews.get(clientId);
        if (webView != null) {
            tinyDB.putString("last_url", webView.getUrl());
        }
    }

    private void loadClientState(int clientId) {
        TinyDB tinyDB = new TinyDB(this, "client_prefs_" + clientId);
        WebView webView = webViews.get(clientId);
        if (webView != null) {
            String lastUrl = tinyDB.getString("last_url");
            if (!lastUrl.isEmpty()) {
                webView.loadUrl(lastUrl);
            }
        }
    }

    private final Map<String, Handler> timedRepeatMacroHandlers = new HashMap<>();

    private void dispatchKeyEvent(WebView webView, ActionButtonData buttonData) {
        switch (buttonData.macroType) {
            case ActionButtonData.TYPE_NORMAL:
                dispatchSingleKeyEvent(webView, buttonData.keyCode);
                break;
            case ActionButtonData.TYPE_MACRO:
                executeMacro(webView, buttonData);
                break;
            case ActionButtonData.TYPE_TIMED_REPEAT_MACRO:
                toggleTimedRepeatMacro(webView, buttonData);
                break;
        }
    }

    private void dispatchSingleKeyEvent(WebView webView, int keyCode) {
        String key;
        String code;
        switch (keyCode) {
            case KeyEvent.KEYCODE_F1: key = "F1"; code = "F1"; break;
            case KeyEvent.KEYCODE_F2: key = "F2"; code = "F2"; break;
            case KeyEvent.KEYCODE_F3: key = "F3"; code = "F3"; break;
            case KeyEvent.KEYCODE_F4: key = "F4"; code = "F4"; break;
            case KeyEvent.KEYCODE_F5: key = "F5"; code = "F5"; break;
            case KeyEvent.KEYCODE_F6: key = "F6"; code = "F6"; break;
            case KeyEvent.KEYCODE_F7: key = "F7"; code = "F7"; break;
            case KeyEvent.KEYCODE_F8: key = "F8"; code = "F8"; break;
            case KeyEvent.KEYCODE_F9: key = "F9"; code = "F9"; break;
            case KeyEvent.KEYCODE_F10: key = "F10"; code = "F10"; break;
            case KeyEvent.KEYCODE_F11: key = "F11"; code = "F11"; break;
            case KeyEvent.KEYCODE_F12: key = "F12"; code = "F12"; break;
            default:
                key = String.valueOf((char) (new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)).getUnicodeChar());
                code = "Key" + key.toUpperCase();
                break;
        }

        String script = "javascript:(function() {" +
                "var canvas = document.querySelector('canvas');" +
                "if (canvas) {" +
                "   var eventProps = { bubbles: true, cancelable: true, key: '" + key + "', code: '" + code + "', keyCode: " + keyCode + " };" +
                "   canvas.dispatchEvent(new KeyboardEvent('keydown', eventProps));" +
                "   canvas.dispatchEvent(new KeyboardEvent('keyup', eventProps));" +
                "}" +
                "})()";
        webView.evaluateJavascript(script, null);
    }

    private void executeMacro(WebView webView, ActionButtonData buttonData) {
        String[] keys = buttonData.macroKeys.split(",");
        Handler handler = new Handler();
        for (int i = 0; i < keys.length; i++) {
            final int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + keys[i].trim().toUpperCase());
            final int delay = (int) (buttonData.delayBetweenKeys * 1000 * i);
            handler.postDelayed(() -> dispatchSingleKeyEvent(webView, keyCode), delay);
        }
    }

    private void toggleTimedRepeatMacro(WebView webView, ActionButtonData buttonData) {
        buttonData.isToggleOn = !buttonData.isToggleOn;

        // Update the ActionButtonData object in clientActionButtonsData
        List<ActionButtonData> clientButtons = clientActionButtonsData.get(buttonData.clientId);
        if (clientButtons != null) {
            for (int i = 0; i < clientButtons.size(); i++) {
                if (clientButtons.get(i).keyText.equals(buttonData.keyText) && clientButtons.get(i).clientId == buttonData.clientId) {
                    clientButtons.set(i, buttonData);
                    break;
                }
            }
        }
        saveActionButtonsState(buttonData.clientId);
        if (buttonData.isToggleOn) {
            // Start repeating
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    dispatchSingleKeyEvent(webView, buttonData.repeatKey);
                    handler.postDelayed(this, (long) (buttonData.repeatInterval * 1000));
                }
            };
            timedRepeatMacroHandlers.put(buttonData.keyText, handler);
            // Initial dispatch
            dispatchSingleKeyEvent(webView, buttonData.repeatKey);
            handler.postDelayed(runnable, (long) (buttonData.repeatInterval * 1000));
        } else {
            // Stop repeating
            if (timedRepeatMacroHandlers.containsKey(buttonData.keyText)) {
                timedRepeatMacroHandlers.get(buttonData.keyText).removeCallbacksAndMessages(null);
                timedRepeatMacroHandlers.remove(buttonData.keyText);
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

    /* ---------- lifecycle ---------- */
    private void fullScreenOn() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    @Override public void onBackPressed() {
        if (exit) { finish(); return; }
        Toast.makeText(this, "Press Back again to Exit", Toast.LENGTH_SHORT).show();
        exit = true;
        new Handler().postDelayed(() -> exit = false, 3000);
    }

    @Override protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (int clientId : configuredClientIds) {
            if (clientActionButtonsData.containsKey(clientId)) {
                saveActionButtonsState(clientId);
            }
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < webViews.size(); i++) webViews.valueAt(i).destroy();
        webViews.clear();
        layouts.clear();
        deleteAllCustomFabs();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("activeClientId", activeClientId);
    }

    @Override protected void onRestoreInstanceState(@NonNull Bundle in) {
        super.onRestoreInstanceState(in);
        activeClientId = in.getInt("activeClientId", -1);
        if (activeClientId != -1) openClient(activeClientId);
    }
}

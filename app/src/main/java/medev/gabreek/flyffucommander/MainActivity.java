package medev.gabreek.flyffucommander;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;



public class MainActivity extends AppCompatActivity implements FabMovementHandler.FabPositionSaver {

    // ... (existing variables) ...
    

    

    // ... (existing methods) ...

    

    

    private final SparseArray<WebView> webViews = new SparseArray<>();
    private final SparseArray<FrameLayout> layouts = new SparseArray<>();
    private int activeClientId = -1;
    private TinyDB appTinyDB;
    private final Set<Integer> configuredClientIds = new HashSet<>();

    private LinearLayout linearLayout;
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton fabHideShow;
    private boolean isActionButtonsVisible = true;
    private boolean areActionButtonsPositionsFixed = false;
    private FrameLayout rootContainer;
    private final Map<Integer, List<ActionButtonData>> clientActionButtonsData = new HashMap<>();
    private final Map<View, ActionButtonData> fabViewToActionDataMap = new HashMap<>();
    private final Map<String, Integer> keyCodeMap = new HashMap<>();
    private Gson gson = new Gson();

    private String userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.5304.105 Mobile Safari/537.36";
    private String url = "https://universe.flyff.com/play";
    private boolean exit = false;

    private ActionButtonManager actionButtonManager;
    private ClientManager clientManager;
    private FabMovementHandler fabMovementHandler;
    private KeyDispatcher keyDispatcher;
    private DisplayUtils displayUtils;
    private BackupManager backupManager;
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("FlyffU Android");

        appTinyDB = new TinyDB(this, "app_prefs");
        isActionButtonsVisible = appTinyDB.getBoolean("isActionButtonsVisible");
        areActionButtonsPositionsFixed = appTinyDB.getBoolean("areActionButtonsPositionsFixed");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        fullScreenOn();

        rootContainer = findViewById(R.id.root_container);
        linearLayout = findViewById(R.id.linearLayout);
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setAlpha(0.5f);
        fabHideShow = findViewById(R.id.fab_hide_show);
        fabHideShow.setAlpha(0.5f);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        displayUtils = new DisplayUtils(this);
        keyDispatcher = new KeyDispatcher(this, webViews, clientActionButtonsData, fabViewToActionDataMap);
        actionButtonManager = new ActionButtonManager(this, rootContainer, clientActionButtonsData, fabViewToActionDataMap, appTinyDB, displayUtils, areActionButtonsPositionsFixed, this::getClientDisplayName, this::getWebViews);
        actionButtonManager.setKeyDispatcher(keyDispatcher);
        keyDispatcher.setActionButtonManager(actionButtonManager);
        clientManager = new ClientManager(this, webViews, layouts, appTinyDB, configuredClientIds, linearLayout, floatingActionButton, actionButtonManager, this::createWebViewer, id -> {
            switch (id) {
                case Constants.WIKI_CLIENT_ID:
                    return "Wiki";
                case Constants.MADRIGAL_CLIENT_ID:
                    return "Madrigal";
                case Constants.FLYFFULATOR_CLIENT_ID:
                    return "Flyffulator";
                case Constants.FLYFFUSKILL_CLIENT_ID:
                    return "FlyffuSkill";
                default:
                    return "Client " + id;
            }
        }, this::setTitle, this::getScreenHeight, this::getScreenWidth, () -> isActionButtonsVisible);
        fabMovementHandler = new FabMovementHandler(screenWidth, screenHeight);
        backupManager = new BackupManager(this, gson, clientActionButtonsData, () -> actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, activeClientId));

        initializeKeyCodeMap();
        setupFabTouchListeners();

        List<Integer> storedClientIds = appTinyDB.getListInt("configuredClientIds");
        if (storedClientIds != null && !storedClientIds.isEmpty()) {
            configuredClientIds.addAll(storedClientIds);
        } else {
            configuredClientIds.addAll(clientManager.getExistingClientIdsFromFileSystem());
        }

        for (int clientId : configuredClientIds) {
            actionButtonManager.loadActionButtonsState(clientId);
        }

        actionButtonManager.resetAllTogglesState();

        if (savedInstanceState == null) {
            if (configuredClientIds.isEmpty()) {
                clientManager.createNewClient();
            } else {
                int first = Collections.min(configuredClientIds);
                clientManager.openClient(first);
            }
        } else {
            activeClientId = savedInstanceState.getInt("activeClientId", -1);
            clientManager.setActiveClientId(activeClientId);
        }
        actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, activeClientId);
    }

    private void setupFabTouchListeners() {
        fabMovementHandler.setupFabTouchListener(floatingActionButton,
                v -> clientManager.switchToNextClient(),
                v -> {
                    showClientManagerMenu(v);
                    return true;
                });

        fabHideShow.setImageResource(isActionButtonsVisible ? R.drawable.ic_hide_show : R.drawable.ic_show_hide);
        fabHideShow.setOnClickListener(v -> {
            isActionButtonsVisible = !isActionButtonsVisible;
            appTinyDB.putBoolean("isActionButtonsVisible", isActionButtonsVisible);
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, activeClientId);
        });

        fabHideShow.setOnLongClickListener(v -> {
            areActionButtonsPositionsFixed = !areActionButtonsPositionsFixed;
            appTinyDB.putBoolean("areActionButtonsPositionsFixed", areActionButtonsPositionsFixed);
            actionButtonManager.setActionButtonsPositionsFixed(areActionButtonsPositionsFixed);
            Toast.makeText(this, "Action Button positions " + (areActionButtonsPositionsFixed ? "fixed" : "draggable"), Toast.LENGTH_SHORT).show();
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, activeClientId);
            return true;
        });

        fabHideShow.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fabHideShow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                float fabHideShowX = appTinyDB.getFloat("fabHideShow_x");
                float fabHideShowY = appTinyDB.getFloat("fabHideShow_y");
                if (fabHideShowX != 0f || fabHideShowY != 0f) {
                    fabHideShow.setX(fabHideShowX);
                    fabHideShow.setY(fabHideShowY);
                }
            }
        });

        fabMovementHandler.setupDraggableFab(fabHideShow, this);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public void createWebViewer(WebView webView, FrameLayout frameLayout, int clientId, String initialUrl) {
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
                view.evaluateJavascript(
                        "(function(){"
                        + "window.localStorage.setItem=function(k,v){AndroidLocalStorage.setItem(k,v)};"
                        + "window.localStorage.getItem=function(k){return AndroidLocalStorage.getItem(k)};"
                        + "window.localStorage.removeItem=function(k){AndroidLocalStorage.removeItem(k)};"
                        + "window.localStorage.clear=function(){AndroidLocalStorage.clear()};"
                        + "})()", null);
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

        if (clientId == Constants.WIKI_CLIENT_ID || clientId == Constants.MADRIGAL_CLIENT_ID || clientId == Constants.FLYFFULATOR_CLIENT_ID || clientId == Constants.FLYFFUSKILL_CLIENT_ID) {
            addUtilityFabs(frameLayout, webView, clientId);
        }
    }

    private void addUtilityFabs(FrameLayout frameLayout, WebView webView, int clientId) {
        int fabMargin = displayUtils.dpToPx(10);
        int fabSize = displayUtils.dpToPx(40);

        FloatingActionButton fabReload = createUtilityFab(fabMargin, fabSize, Gravity.TOP | Gravity.END, 0, android.R.drawable.ic_menu_rotate);
        fabReload.setOnClickListener(v -> webView.reload());
        frameLayout.addView(fabReload);

        FloatingActionButton fabBack = createUtilityFab(fabMargin, fabSize, Gravity.TOP | Gravity.END, fabMargin + fabSize + fabMargin, android.R.drawable.ic_media_previous);
        fabBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        frameLayout.addView(fabBack);

        FloatingActionButton fabKill = createUtilityFab(fabMargin, fabSize, Gravity.TOP | Gravity.END, fabMargin + (fabSize + fabMargin) * 2, android.R.drawable.ic_delete);
        fabKill.setOnClickListener(v -> clientManager.confirmCloseUtilityClient(clientId));
        frameLayout.addView(fabKill);
    }

    private FloatingActionButton createUtilityFab(int margin, int size, int gravity, int topMargin, int imageResource) {
        FloatingActionButton fab = new FloatingActionButton(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = gravity;
        params.setMargins(0, topMargin, margin, 0);
        fab.setLayoutParams(params);
        fab.setImageResource(imageResource);
        fab.setAlpha(0.5f);
        android.graphics.drawable.GradientDrawable circularBackground = new android.graphics.drawable.GradientDrawable();
        circularBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circularBackground.setColor(Color.WHITE);
        fab.setBackground(circularBackground);
        return fab;
    }

    private void showClientManagerMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "New Client");

        List<Integer> sorted = new ArrayList<>(configuredClientIds);
        Collections.sort(sorted);
        if (!sorted.isEmpty()) {
            SubMenu sub = popup.getMenu().addSubMenu(Menu.NONE, 2, Menu.NONE, "Clients");
            for (int id : sorted) {
                SubMenu sm = sub.addSubMenu(clientManager.getClientDisplayName(id));
                boolean open = webViews.get(id) != null;
                if (open) {
                    sm.add(Menu.NONE, 6000 + id, 0, "Action Buttons");
                    sm.add(Menu.NONE, 1000 + id, 1, "Switch");
                    sm.add(Menu.NONE, 2000 + id, 2, "Kill");
                } else {
                    sm.add(Menu.NONE, 3000 + id, 1, "Open");
                }
                sm.add(Menu.NONE, 4000 + id, 3, "Rename");
                sm.add(Menu.NONE, 5000 + id, 4, "Delete");
            }
        }
        SubMenu util = popup.getMenu().addSubMenu(Menu.NONE, 3, Menu.NONE, "Utils");
        util.add(Menu.NONE, 7000 + Math.abs(Constants.WIKI_CLIENT_ID), Menu.NONE, "Flyffipedia");
        util.add(Menu.NONE, 7000 + Math.abs(Constants.MADRIGAL_CLIENT_ID), Menu.NONE, "Madrigal Inside");
        util.add(Menu.NONE, 7000 + Math.abs(Constants.FLYFFULATOR_CLIENT_ID), Menu.NONE, "Flyffulator");
        util.add(Menu.NONE, 7000 + Math.abs(Constants.FLYFFUSKILL_CLIENT_ID), Menu.NONE, "Flyff Skill Simulator");

        popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Backup / Restore");

        popup.setOnMenuItemClickListener(item -> {
            handleMenuClick(item);
            return true;
        });
        popup.show();
    }

    private void handleMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        int id = -1;

        if (itemId > 1000) {
            if (itemId < 2000) id = itemId - 1000;
            else if (itemId < 3000) id = itemId - 2000;
            else if (itemId < 4000) id = itemId - 3000;
            else if (itemId < 5000) id = itemId - 4000;
            else if (itemId < 6000) id = itemId - 5000;
            else if (itemId < 7000) id = itemId - 6000;
            else if (itemId < 8000) id = -(itemId - 7000);
        }

        if (itemId == 1) {
            clientManager.createNewClient();
        } else if (itemId == 4) { // Backup / Restore
            backupManager.showBackupRestoreDialog();
        } else if (id != -1) {
            if (itemId >= 1000 && itemId < 2000) clientManager.switchToClient(id);
            else if (itemId >= 2000 && itemId < 3000) clientManager.confirmKillClient(id);
            else if (itemId >= 3000 && itemId < 4000) clientManager.openClient(id);
            else if (itemId >= 4000 && itemId < 5000) showRenameDialog(id);
            else if (itemId >= 5000 && itemId < 6000) clientManager.confirmDeleteClient(id);
            else if (itemId >= 6000 && itemId < 7000) {
                clientManager.switchToClient(id);
                showActionButtonsMenu();
            } else if (itemId >= 7000 && itemId < 8000) clientManager.openUtilityClient(id);
        }
    }

    private void showRenameDialog(int id) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Rename Client");
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(clientManager.getClientDisplayName(id));
        b.setView(input);
        b.setPositiveButton("Save", (d, w) -> {
            String n = input.getText().toString().trim();
            if (!n.isEmpty()) {
                TinyDB db = new TinyDB(this, "client_prefs_" + id);
                db.putString(Constants.CLIENT_NAME_KEY, n);
                Toast.makeText(this, "Renamed to " + n, Toast.LENGTH_SHORT).show();
                if (clientManager.getActiveClientId() == id) setTitle(n);
            } else {
                Toast.makeText(this, "Empty name", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
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
                showColorSelectionDialog(clientManager.getActiveClientId());
            } else if (selectedOption.equals("Delete")) {
                showDeleteActionButtonDialog(clientManager.getActiveClientId());
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
                showFunctionKeyOptionsDialog();
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

    private void showFunctionKeyOptionsDialog() {
        final CharSequence[] items = {"Single Button (change active bar)", "Combo Button"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Function Key Type");
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Single Button (change active bar)")) {
                showFunctionKeyDialog();
            } else if (items[item].equals("Combo Button")) {
                showComboButtonDialog();
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
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;
            ActionButtonData newButtonData = new ActionButtonData(key, (int) keyCodeMap.get(key), centerX, centerY, Color.BLACK, clientManager.getActiveClientId());
            actionButtonManager.createCustomFab(newButtonData);
            actionButtonManager.saveActionButtonsState(clientManager.getActiveClientId());
            isActionButtonsVisible = true;
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientManager.getActiveClientId());
            Toast.makeText(this, "Action Button for '" + newButtonData.keyText + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void showComboButtonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Combo Button");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final CharSequence[] fKeys = new CharSequence[12];
        for (int i = 0; i < 12; i++) {
            fKeys[i] = "F" + (i + 1);
        }

        TextView mainKeyLabel = new TextView(this);
        mainKeyLabel.setText("Select Main Function Key:");
        layout.addView(mainKeyLabel);
        final Spinner mainKeySpinner = new Spinner(this);
        ArrayAdapter<CharSequence> mainKeyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fKeys);
        mainKeyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainKeySpinner.setAdapter(mainKeyAdapter);
        layout.addView(mainKeySpinner);

        final EditText digitInput = new EditText(this);
        digitInput.setHint("Enter Digit Key (0-9)");
        digitInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        digitInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1), new InputFilterMinMax("0", "9")});
        layout.addView(digitInput);

        TextView afterPressedLabel = new TextView(this);
        afterPressedLabel.setText("After Pressed Go To Bar:");
        layout.addView(afterPressedLabel);
        final Spinner afterPressedSpinner = new Spinner(this);
        ArrayAdapter<CharSequence> afterPressedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fKeys);
        afterPressedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        afterPressedSpinner.setAdapter(afterPressedAdapter);
        layout.addView(afterPressedSpinner);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String mainKeyText = fKeys[mainKeySpinner.getSelectedItemPosition()].toString();
            String digitKeyText = digitInput.getText().toString();
            String afterPressedKeyText = fKeys[afterPressedSpinner.getSelectedItemPosition()].toString();

            if (digitKeyText.isEmpty()) {
                Toast.makeText(this, "Digit key cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int mainKeyCode = keyCodeMap.get(mainKeyText);
            int digitKeyCode = KeyEvent.keyCodeFromString("KEYCODE_NUMPAD_" + digitKeyText);
            if (digitKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
                digitKeyCode = KeyEvent.keyCodeFromString("KEYCODE_" + digitKeyText);
            }
            int afterPressedKeyCode = keyCodeMap.get(afterPressedKeyText);

            String keyText = mainKeyText + "+" + digitKeyText + "->" + afterPressedKeyText;
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;

            ActionButtonData newButtonData = new ActionButtonData(
                    keyText, 0, centerX, centerY, Color.BLACK, clientManager.getActiveClientId(),
                    ActionButtonData.TYPE_COMBO, null, 0.0f, 0, 0.0f, false, false, false,
                    mainKeyCode, digitKeyCode, afterPressedKeyCode
            );
            actionButtonManager.createCustomFab(newButtonData);
            actionButtonManager.saveActionButtonsState(clientManager.getActiveClientId());
            isActionButtonsVisible = true;
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientManager.getActiveClientId());
            Toast.makeText(this, "Combo Button '" + newButtonData.keyText + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showCustomKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Custom Key");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(input);

        final CheckBox altCheckBox = new CheckBox(this);
        altCheckBox.setText("Alt");
        layout.addView(altCheckBox);

        final CheckBox ctrlCheckBox = new CheckBox(this);
        ctrlCheckBox.setText("Ctrl");
        layout.addView(ctrlCheckBox);

        altCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ctrlCheckBox.setChecked(false);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1), new InputFilterMinMax("0", "9")});
            } else if (!ctrlCheckBox.isChecked()) {
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setFilters(new InputFilter[]{});
            }
        });

        ctrlCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                altCheckBox.setChecked(false);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1), new InputFilterMinMax("0", "9")});
            } else if (!altCheckBox.isChecked()) {
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setFilters(new InputFilter[]{});
            }
        });

        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String key = input.getText().toString().toUpperCase();
            boolean isAlt = altCheckBox.isChecked();
            boolean isCtrl = ctrlCheckBox.isChecked();

            if (key.isEmpty()) {
                Toast.makeText(this, "Key cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int keyCode;
            if (key.length() == 1) {
                keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key);
            } else {
                Toast.makeText(this, "Please enter a single character", Toast.LENGTH_SHORT).show();
                return;
            }

            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                float centerX = screenWidth / 2f;
                float centerY = screenHeight / 2f;
                ActionButtonData newButtonData = new ActionButtonData(key, keyCode, centerX, centerY, Color.BLACK, clientManager.getActiveClientId(), ActionButtonData.TYPE_NORMAL, null, 0.0f, 0, 0.0f, false, isAlt, isCtrl);
                actionButtonManager.createCustomFab(newButtonData);
                actionButtonManager.saveActionButtonsState(clientManager.getActiveClientId());
                isActionButtonsVisible = true;
                actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientManager.getActiveClientId());
                Toast.makeText(this, "Action Button for '" + newButtonData.keyText + "' created.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
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
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        layout.addView(nameInput);

        final EditText keysInput = new EditText(this);
        keysInput.setHint("Keys (e.g., 1,2,3)");
        keysInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(keysInput);

        final CheckBox altCheckBox = new CheckBox(this);
        altCheckBox.setText("Alt");
        layout.addView(altCheckBox);

        final CheckBox ctrlCheckBox = new CheckBox(this);
        ctrlCheckBox.setText("Ctrl");
        layout.addView(ctrlCheckBox);

        altCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ctrlCheckBox.setChecked(false);
            }
        });

        ctrlCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                altCheckBox.setChecked(false);
            }
        });

        TextView delayLabel = new TextView(this);
        delayLabel.setText("Delay between keys: 0.5s");
        layout.addView(delayLabel);

        SeekBar delaySlider = new SeekBar(this);
        delaySlider.setMax(45);
        delaySlider.setProgress(0);
        delaySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float delay = 0.5f + progress * 0.1f;
                delayLabel.setText("Delay between keys: " + new DecimalFormat("0.0").format(delay) + "s");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(delaySlider);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().toUpperCase();
            String keys = keysInput.getText().toString();
            float delay = 0.5f + delaySlider.getProgress() * 0.1f;
            boolean isAlt = altCheckBox.isChecked();
            boolean isCtrl = ctrlCheckBox.isChecked();

            if (name.isEmpty() || keys.isEmpty()) {
                Toast.makeText(this, "Name and keys cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;
            ActionButtonData newButtonData = new ActionButtonData(name, 0, centerX, centerY, Color.BLACK, clientManager.getActiveClientId(), ActionButtonData.TYPE_MACRO, keys, delay, 0, 0.0f, false, isAlt, isCtrl);
            actionButtonManager.createCustomFab(newButtonData);
            actionButtonManager.saveActionButtonsState(clientManager.getActiveClientId());
            isActionButtonsVisible = true;
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientManager.getActiveClientId());
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
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        layout.addView(nameInput);

        final EditText keyInput = new EditText(this);
        keyInput.setHint("Single Key (e.g., 1)");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT);
        keyInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        layout.addView(keyInput);

        final CheckBox altCheckBox = new CheckBox(this);
        altCheckBox.setText("Alt");
        layout.addView(altCheckBox);

        final CheckBox ctrlCheckBox = new CheckBox(this);
        ctrlCheckBox.setText("Ctrl");
        layout.addView(ctrlCheckBox);

        altCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ctrlCheckBox.setChecked(false);
            }
        });

        ctrlCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                altCheckBox.setChecked(false);
            }
        });

        TextView intervalLabel = new TextView(this);
        intervalLabel.setText("Repeat Interval: 0.5s");
        layout.addView(intervalLabel);

        SeekBar intervalSlider = new SeekBar(this);
        intervalSlider.setMax(195);
        intervalSlider.setProgress(0);
        intervalSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float interval = 0.5f + progress * 0.1f;
                intervalLabel.setText("Repeat Interval: " + new DecimalFormat("0.0").format(interval) + "s");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(intervalSlider);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = nameInput.getText().toString().toUpperCase();
            String key = keyInput.getText().toString();
            float interval = 0.5f + intervalSlider.getProgress() * 0.1f;
            boolean isAlt = altCheckBox.isChecked();
            boolean isCtrl = ctrlCheckBox.isChecked();

            if (name.isEmpty() || key.isEmpty()) {
                Toast.makeText(this, "Name and key cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int repeatKeyCode;
            if (key.length() == 1) {
                repeatKeyCode = KeyEvent.keyCodeFromString("KEYCODE_" + key.toUpperCase());
            } else {
                Toast.makeText(this, "Key must be a single character", Toast.LENGTH_SHORT).show();
                return;
            }

            if (repeatKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
                Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
                return;
            }

            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;
            ActionButtonData newButtonData = new ActionButtonData(name, 0, centerX, centerY, Color.BLACK, clientManager.getActiveClientId(), ActionButtonData.TYPE_TIMED_REPEAT_MACRO, null, 0.0f, repeatKeyCode, interval, false, isAlt, isCtrl);
            actionButtonManager.createCustomFab(newButtonData);
            actionButtonManager.saveActionButtonsState(clientManager.getActiveClientId());
            isActionButtonsVisible = true;
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientManager.getActiveClientId());
            Toast.makeText(this, "Timed Repeat Macro Button '" + name + "' created.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
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
            if (whichButton == 0) { // "All buttons"
                showColorPickerForMultipleButtons(clientId, clientButtons);
            } else {
                showColorPickerForSingleButton(clientId, clientButtons.get(whichButton - 1));
            }
        });
        builder.show();
    }

    private void showColorPickerForMultipleButtons(int clientId, List<ActionButtonData> buttons) {
        final CharSequence[] colorNames = {"Red", "Green", "Blue", "Black", "White", "Gray"};
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.WHITE, Color.GRAY};

        AlertDialog.Builder colorBuilder = new AlertDialog.Builder(this);
        colorBuilder.setTitle("Select Color for All Buttons");
        colorBuilder.setItems(colorNames, (colorDialog, whichColor) -> {
            int newColor = colors[whichColor];
            for (ActionButtonData data : buttons) {
                data.color = newColor;
            }
            actionButtonManager.saveActionButtonsState(clientId);
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientId);
            Toast.makeText(this, "All buttons colored " + colorNames[whichColor], Toast.LENGTH_SHORT).show();
        });
        colorBuilder.show();
    }

    private void showColorPickerForSingleButton(int clientId, ActionButtonData buttonData) {
        final CharSequence[] colorNames = {"Red", "Green", "Blue", "Black", "White", "Gray"};
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.WHITE, Color.GRAY};

        AlertDialog.Builder colorBuilder = new AlertDialog.Builder(this);
        colorBuilder.setTitle("Select Color for " + buttonData.keyText);
        colorBuilder.setItems(colorNames, (colorDialog, whichColor) -> {
            int newColor = colors[whichColor];
            buttonData.color = newColor;
            actionButtonManager.saveActionButtonsState(clientId);
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientId);
            Toast.makeText(this, buttonData.keyText + " color changed to " + colorNames[whichColor], Toast.LENGTH_SHORT).show();
        });
        colorBuilder.show();
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
            ActionButtonData dataToRemove = clientButtons.get(whichButton);
            clientButtons.remove(dataToRemove);
            actionButtonManager.saveActionButtonsState(clientId);
            actionButtonManager.refreshAllActionButtonsDisplay(isActionButtonsVisible, fabHideShow, clientId);
            Toast.makeText(this, dataToRemove.keyText + " Action Button deleted.", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void initializeKeyCodeMap() {
        for (int i = 0; i < 12; i++) {
            keyCodeMap.put("F" + (i + 1), KeyEvent.KEYCODE_F1 + i);
        }
        keyCodeMap.put("A", KeyEvent.KEYCODE_A);
        keyCodeMap.put("B", KeyEvent.KEYCODE_B);
        // ... and so on
    }

    private void fullScreenOn() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    @Override
    public void onBackPressed() {
        if (exit) {
            finish();
            return;
        }
        Toast.makeText(this, "Press Back again to Exit", Toast.LENGTH_SHORT).show();
        exit = true;
        new Handler().postDelayed(() -> exit = false, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appTinyDB.putBoolean("isActionButtonsVisible", isActionButtonsVisible);
        appTinyDB.putBoolean("areActionButtonsPositionsFixed", areActionButtonsPositionsFixed);
        for (int clientId : configuredClientIds) {
            if (clientActionButtonsData.containsKey(clientId)) {
                actionButtonManager.saveActionButtonsState(clientId);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        appTinyDB.putBoolean("isActionButtonsVisible", isActionButtonsVisible);
        appTinyDB.putBoolean("areActionButtonsPositionsFixed", areActionButtonsPositionsFixed);
        for (int clientId : configuredClientIds) {
            if (clientActionButtonsData.containsKey(clientId)) {
                actionButtonManager.saveActionButtonsState(clientId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < webViews.size(); i++) {
            webViews.valueAt(i).destroy();
        }
        webViews.clear();
        layouts.clear();
        actionButtonManager.deleteAllCustomFabs();
        keyDispatcher.stopAllTimedRepeatMacros();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("activeClientId", clientManager.getActiveClientId());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle in) {
        super.onRestoreInstanceState(in);
        activeClientId = in.getInt("activeClientId", -1);
        clientManager.setActiveClientId(activeClientId);
    }

    @Override
    public void saveFabHideShowPosition(float x, float y) {
        appTinyDB.putFloat("fabHideShow_x", x);
        appTinyDB.putFloat("fabHideShow_y", y);
    }

    public String getClientDisplayName(int id) {
        return clientManager.getClientDisplayName(id);
    }

    public SparseArray<WebView> getWebViews() {
        return webViews;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }
}
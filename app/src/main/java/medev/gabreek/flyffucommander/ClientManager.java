package medev.gabreek.flyffucommander;

import android.app.AlertDialog;
import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientManager {

    private final Context context;
    private final SparseArray<WebView> webViews;
    private final SparseArray<FrameLayout> layouts;
    private int activeClientId = -1;
    private final TinyDB appTinyDB;
    private final Set<Integer> configuredClientIds;
    private final LinearLayout linearLayout;
    private final FloatingActionButton floatingActionButton;
    private final ActionButtonManager actionButtonManager;
    private final WebViewCreator webViewCreator;
    private final Function<Integer, String> clientDisplayNameProvider;
    private final Consumer<String> titleSetter;
    private final Supplier<Integer> screenHeightProvider;
    private final Supplier<Integer> screenWidthProvider;

    @FunctionalInterface
    public interface WebViewCreator {
        void create(WebView webView, FrameLayout frameLayout, int clientId, String initialUrl);
    }

    public ClientManager(Context context, SparseArray<WebView> webViews, SparseArray<FrameLayout> layouts,
                         TinyDB appTinyDB, Set<Integer> configuredClientIds, LinearLayout linearLayout,
                         FloatingActionButton floatingActionButton, ActionButtonManager actionButtonManager,
                         WebViewCreator webViewCreator, Function<Integer, String> clientDisplayNameProvider,
                         Consumer<String> titleSetter, Supplier<Integer> screenHeightProvider, Supplier<Integer> screenWidthProvider) {
        this.context = context;
        this.webViews = webViews;
        this.layouts = layouts;
        this.appTinyDB = appTinyDB;
        this.configuredClientIds = configuredClientIds;
        this.linearLayout = linearLayout;
        this.floatingActionButton = floatingActionButton;
        this.actionButtonManager = actionButtonManager;
        this.webViewCreator = webViewCreator;
        this.clientDisplayNameProvider = clientDisplayNameProvider;
        this.titleSetter = titleSetter;
        this.screenHeightProvider = screenHeightProvider;
        this.screenWidthProvider = screenWidthProvider;
    }

    public int getActiveClientId() {
        return activeClientId;
    }

    public void setActiveClientId(int activeClientId) {
        this.activeClientId = activeClientId;
    }

    public void openClient(int id) {
        if (webViews.get(id) != null) {
            switchToClient(id);
            return;
        }
        if (webViews.size() >= Constants.MAX_CLIENTS) {
            Toast.makeText(context, "Max clients open", Toast.LENGTH_SHORT).show();
            return;
        }
        FrameLayout fl = new FrameLayout(context);
        fl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(fl);
        layouts.put(id, fl);

        WebView w = new CustomWebView(context);
        webViewCreator.create(w, fl, id, "https://universe.flyff.com/play");
        webViews.put(id, w);
        switchToClient(id);
        floatingActionButton.setVisibility(View.VISIBLE);
        actionButtonManager.refreshAllActionButtonsDisplay(true, null, id);
    }

    public void switchToClient(int id) {
        if (layouts.get(id) == null) {
            if (webViews.size() > 0) {
                switchToClient(webViews.keyAt(0));
            } else {
                activeClientId = -1;
                titleSetter.accept("FlyffU Android");
                floatingActionButton.setVisibility(View.GONE);
            }
            return;
        }

        if (activeClientId != -1 && activeClientId != id) {
            actionButtonManager.saveActionButtonsState(activeClientId);
        }

        for (int i = 0; i < layouts.size(); i++) {
            int k = layouts.keyAt(i);
            layouts.get(k).setVisibility(k == id ? View.VISIBLE : View.GONE);
        }
        activeClientId = id;
        actionButtonManager.loadActionButtonsState(activeClientId);
        titleSetter.accept(getClientDisplayName(id));
        WebView w = webViews.get(id);
        if (w != null) {
            w.requestFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(w.getWindowToken(), 0);
            }
        }
        actionButtonManager.refreshAllActionButtonsDisplay(true, null, id);
    }

    public void switchToNextClient() {
        if (webViews.size() > 1) {
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < webViews.size(); i++) {
                ids.add(webViews.keyAt(i));
            }
            Collections.sort(ids);
            int idx = (ids.indexOf(activeClientId) + 1) % ids.size();
            switchToClient(ids.get(idx));
        }
    }

    public void killClient(int id) {
        if (webViews.get(id) == null) return;
        WebView w = webViews.get(id);
        FrameLayout f = layouts.get(id);
        f.removeAllViews();
        w.destroy();
        linearLayout.removeView(f);
        webViews.remove(id);
        layouts.remove(id);
        Toast.makeText(context, getClientDisplayName(id) + " killed", Toast.LENGTH_SHORT).show();
        if (webViews.size() == 0) {
            activeClientId = -1;
            titleSetter.accept("FlyffU Android");
            floatingActionButton.setVisibility(View.GONE);
        } else if (activeClientId == id) {
            activeClientId = webViews.keyAt(0);
            switchToClient(activeClientId);
        }
        actionButtonManager.refreshAllActionButtonsDisplay(true, null, activeClientId);
    }

    public void deleteClient(int id) {
        if (webViews.get(id) != null) {
            killClient(id);
        }
        configuredClientIds.remove(id);
        appTinyDB.putListInt("configuredClientIds", new ArrayList<>(configuredClientIds));
        TinyDB db = new TinyDB(context, "client_prefs_" + id);
        db.clear();
        File f = new File(context.getApplicationInfo().dataDir, "shared_prefs/client_prefs_" + id + ".xml");
        if (f.exists()) {
            f.delete();
        }
        Toast.makeText(context, "Client " + id + " deleted", Toast.LENGTH_SHORT).show();
        if (configuredClientIds.isEmpty()) {
            createNewClient();
        }
    }

    public List<Integer> getExistingClientIdsFromFileSystem() {
        List<Integer> ids = new ArrayList<>();
        File dir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        if (dir.exists() && dir.isDirectory()) {
            Pattern p = Pattern.compile("client_prefs_(\\d+)\\.xml");
            File[] fs = dir.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    Matcher m = p.matcher(f.getName());
                    if (m.matches()) {
                        try {
                            ids.add(Integer.parseInt(m.group(1)));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            }
        }
        return ids;
    }

    public String getClientDisplayName(int id) {
        return clientDisplayNameProvider.apply(id);
    }

    public void createNewClient() {
        // First, try to open an existing, but currently closed, client
        for (int id : configuredClientIds) {
            if (webViews.get(id) == null) { // Client is configured but not currently open
                openClient(id);
                Toast.makeText(context, "Client " + getClientDisplayName(id) + " opened", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // If all configured clients are open or there are no configured clients, create a new one
        if (webViews.size() >= Constants.MAX_CLIENTS) {
            Toast.makeText(context, "Max clients reached", Toast.LENGTH_SHORT).show();
            return;
        }
        int newId = 1;
        while (configuredClientIds.contains(newId) && newId <= Constants.MAX_CLIENTS) {
            newId++;
        }
        if (newId > Constants.MAX_CLIENTS) {
            Toast.makeText(context, "No free slot", Toast.LENGTH_SHORT).show();
            return;
        }
        configuredClientIds.add(newId);
        appTinyDB.putListInt("configuredClientIds", new ArrayList<>(configuredClientIds));
        openClient(newId);
        Toast.makeText(context, "Client " + getClientDisplayName(newId) + " created", Toast.LENGTH_SHORT).show();
    }

    public void openUtilityClient(int id) {
        String u;
        switch (id) {
            case Constants.WIKI_CLIENT_ID:
                u = Constants.WIKI_URL;
                break;
            case Constants.MADRIGAL_CLIENT_ID:
                u = Constants.MADRIGAL_URL;
                break;
            case Constants.FLYFFULATOR_CLIENT_ID:
                u = Constants.FLYFFULATOR_URL;
                break;
            case Constants.FLYFFUSKILL_CLIENT_ID:
                u = Constants.FLYFFUSKILL_URL;
                break;
            default:
                return;
        }
        if (webViews.get(id) != null) {
            if (activeClientId == id) {
                confirmCloseUtilityClient(id);
            } else {
                switchToClient(id);
            }
            return;
        }
        if (webViews.size() >= Constants.MAX_CLIENTS) {
            Toast.makeText(context, "Max clients reached", Toast.LENGTH_SHORT).show();
            return;
        }
        FrameLayout fl = new FrameLayout(context);
        fl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(fl);
        layouts.put(id, fl);

        WebView w = new CustomWebView(context);
        webViewCreator.create(w, fl, id, u);
        webViews.put(id, w);
        switchToClient(id);
        floatingActionButton.setVisibility(View.VISIBLE);
        Toast.makeText(context, getClientDisplayName(id) + " opened", Toast.LENGTH_SHORT).show();
    }

    public void confirmKillClient(int id) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Kill?")
                .setMessage("Kill " + getClientDisplayName(id) + "?")
                .setPositiveButton("Yes", (d, w) -> killClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    public void confirmDeleteClient(int id) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Delete?")
                .setMessage("Delete " + getClientDisplayName(id) + "? This is permanent.")
                .setPositiveButton("Yes", (d, w) -> deleteClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    public void confirmCloseUtilityClient(int id) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Close?")
                .setMessage("Close " + getClientDisplayName(id) + "?")
                .setPositiveButton("Yes", (d, w) -> closeUtilityClient(id))
                .setNegativeButton("No", null)
                .show();
    }

    public void closeUtilityClient(int id) {
        if (webViews.get(id) == null) return;
        WebView w = webViews.get(id);
        FrameLayout f = layouts.get(id);
        f.removeAllViews();
        w.destroy();
        linearLayout.removeView(f);
        webViews.remove(id);
        layouts.remove(id);
        Toast.makeText(context, getClientDisplayName(id) + " closed", Toast.LENGTH_SHORT).show();
        if (webViews.size() == 0) {
            activeClientId = -1;
            titleSetter.accept("FlyffU Android");
            floatingActionButton.setVisibility(View.GONE);
        } else {
            activeClientId = webViews.keyAt(0);
            switchToClient(activeClientId);
        }
    }
}

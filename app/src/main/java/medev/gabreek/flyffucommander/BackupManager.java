package medev.gabreek.flyffucommander;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupManager {

    private final AppCompatActivity activity;
    private final Gson gson;
    private final Map<Integer, List<ActionButtonData>> clientActionButtonsData;
    private final Runnable refreshCallback;
    private final ActivityResultLauncher<Intent> backupLauncher;
    private final ActivityResultLauncher<Intent> restoreLauncher;

    public BackupManager(AppCompatActivity activity, Gson gson, Map<Integer, List<ActionButtonData>> clientActionButtonsData, Runnable refreshCallback) {
        this.activity = activity;
        this.gson = gson;
        this.clientActionButtonsData = clientActionButtonsData;
        this.refreshCallback = refreshCallback;

        this.backupLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            writeBackupToFile(uri);
                        }
                    }
                });

        this.restoreLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            readBackupFromFile(uri);
                        }
                    }
                });
    }

    public void showBackupRestoreDialog() {
        final CharSequence[] items = {"Backup All Action Buttons", "Restore Action Buttons"};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Backup & Restore");
        builder.setItems(items, (dialog, item) -> {
            if (item == 0) { // Backup
                performBackup();
            } else { // Restore
                performRestore();
            }
        });
        builder.show();
    }

    private void performBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "flyffu_action_buttons_backup.json");
        backupLauncher.launch(intent);
    }

    private void writeBackupToFile(Uri uri) {
        try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
            Map<String, List<ActionButtonData>> backupData = new HashMap<>();
            for (Map.Entry<Integer, List<ActionButtonData>> entry : clientActionButtonsData.entrySet()) {
                backupData.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            String json = gson.toJson(backupData);
            outputStream.write(json.getBytes());
            Toast.makeText(activity, "Backup successful!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(activity, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void performRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }

    private void readBackupFromFile(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = activity.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            confirmRestore(stringBuilder.toString());
        } catch (IOException e) {
            Toast.makeText(activity, "Failed to read backup file.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRestore(String json) {
        new AlertDialog.Builder(activity)
                .setTitle("Confirm Restore")
                .setMessage("This will overwrite ALL existing action buttons. This action cannot be undone. Are you sure?")
                .setPositiveButton("Restore", (dialog, which) -> applyRestore(json))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyRestore(String json) {
        try {
            Type type = new TypeToken<Map<String, List<ActionButtonData>>>() {}.getType();
            Map<String, List<ActionButtonData>> restoredData = gson.fromJson(json, type);

            if (restoredData == null) {
                Toast.makeText(activity, "Invalid backup file format.", Toast.LENGTH_SHORT).show();
                return;
            }

            clientActionButtonsData.clear();
            for (Map.Entry<String, List<ActionButtonData>> entry : restoredData.entrySet()) {
                try {
                    int clientId = Integer.parseInt(entry.getKey());
                    clientActionButtonsData.put(clientId, entry.getValue());
                    // Save state for each client after restoring
                    TinyDB tinyDB = new TinyDB(activity, "client_prefs_" + clientId);
                    tinyDB.putString(Constants.ACTION_BUTTONS_DATA_KEY, gson.toJson(entry.getValue()));
                } catch (NumberFormatException e) {
                    // Safely ignore entries with invalid client IDs
                }
            }

            refreshCallback.run();
            Toast.makeText(activity, "Restore successful!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "Restore failed: Invalid file content.", Toast.LENGTH_LONG).show();
        }
    }
}

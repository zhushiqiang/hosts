package com.carhosts.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MainActivity - UI for CarHostsApp
 * Designed for Android 9 car systems with touch interface
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CarHostsApp";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private HostsManager hostsManager;
    
    // UI Components
    private EditText editIpAddress;
    private EditText editHostname;
    private Button btnAdd;
    private Button btnApply;
    private Button btnLoad;
    private Button btnSave;
    private Button btnClear;
    private TextView txtStatus;
    private TextView txtRootStatus;
    private ListView listViewEntries;
    
    private List<String> displayList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize HostsManager
        hostsManager = new HostsManager(this);
        
        // Initialize UI components
        initViews();
        
        // Request permissions
        requestPermissions();
        
        // Check root status
        checkRootStatus();
        
        // Load existing entries
        loadEntriesToList();
        
        // Setup click listeners
        setupListeners();
    }
    
    private void initViews() {
        editIpAddress = findViewById(R.id.edit_ip_address);
        editHostname = findViewById(R.id.edit_hostname);
        btnAdd = findViewById(R.id.btn_add);
        btnApply = findViewById(R.id.btn_apply);
        btnLoad = findViewById(R.id.btn_load);
        btnSave = findViewById(R.id.btn_save);
        btnClear = findViewById(R.id.btn_clear);
        txtStatus = findViewById(R.id.txt_status);
        txtRootStatus = findViewById(R.id.txt_root_status);
        listViewEntries = findViewById(R.id.list_view_entries);
        
        // Set default values for common car system use cases
        editIpAddress.setText("127.0.0.1");
        
        displayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listViewEntries.setAdapter(adapter);
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            
            if (!permissions.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissions.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    private void checkRootStatus() {
        boolean hasRoot = hostsManager.hasRootAccess();
        if (hasRoot) {
            txtRootStatus.setText("Root Status: ✓ Available");
            txtRootStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            txtRootStatus.setText("Root Status: ✗ Not Available (Limited functionality)");
            txtRootStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    private void setupListeners() {
        // Add entry button
        btnAdd.setOnClickListener(v -> addEntry());
        
        // Apply to system button
        btnApply.setOnClickListener(v -> applyHosts());
        
        // Load from backup button
        btnLoad.setOnClickListener(v -> loadFromBackup());
        
        // Save to backup button
        btnSave.setOnClickListener(v -> saveToBackup());
        
        // Clear all button
        btnClear.setOnClickListener(v -> clearAll());
        
        // List item long click to remove
        listViewEntries.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayList.size()) {
                String item = displayList.get(position);
                // Parse hostname from display string
                String[] parts = item.split(" -> ");
                if (parts.length >= 2) {
                    String hostname = parts[0].trim();
                    hostsManager.removeHostEntry(hostname);
                    loadEntriesToList();
                    Toast.makeText(this, "Removed: " + hostname, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
    }
    
    private void addEntry() {
        String ip = editIpAddress.getText().toString().trim();
        String hostname = editHostname.getText().toString().trim();
        
        if (ip.isEmpty() || hostname.isEmpty()) {
            Toast.makeText(this, "Please enter both IP and hostname", Toast.LENGTH_SHORT).show();
            return;
        }
        
        hostsManager.addHostEntry(ip, hostname);
        loadEntriesToList();
        
        // Clear hostname field for next entry
        editHostname.setText("");
        
        Toast.makeText(this, "Added: " + hostname + " -> " + ip, Toast.LENGTH_SHORT).show();
    }
    
    private void applyHosts() {
        if (!hostsManager.hasRootAccess()) {
            Toast.makeText(this, "Root access required to apply hosts", Toast.LENGTH_LONG).show();
            return;
        }
        
        txtStatus.setText("Applying hosts...");
        
        boolean success = hostsManager.applyHosts();
        
        if (success) {
            txtStatus.setText("✓ Hosts applied successfully");
            Toast.makeText(this, "Hosts file updated!", Toast.LENGTH_LONG).show();
        } else {
            txtStatus.setText("✗ Failed to apply hosts");
            Toast.makeText(this, "Failed to update hosts file", Toast.LENGTH_LONG).show();
        }
    }
    
    private void loadFromBackup() {
        boolean success = hostsManager.loadFromAppStorage();
        
        if (success) {
            loadEntriesToList();
            txtStatus.setText("✓ Loaded from backup");
            Toast.makeText(this, "Loaded " + hostsManager.getEntryCount() + " entries", Toast.LENGTH_SHORT).show();
        } else {
            txtStatus.setText("✗ No backup found");
            Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveToBackup() {
        boolean success = hostsManager.saveToAppStorage();
        
        if (success) {
            txtStatus.setText("✓ Saved to backup");
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show();
        } else {
            txtStatus.setText("✗ Failed to save");
            Toast.makeText(this, "Failed to save configuration", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearAll() {
        hostsManager.clearAllEntries();
        loadEntriesToList();
        txtStatus.setText("Cleared all entries");
        Toast.makeText(this, "All entries cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void loadEntriesToList() {
        displayList.clear();
        Map<String, String> entries = hostsManager.getEntries();
        
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            displayList.add(entry.getKey() + " -> " + entry.getValue());
        }
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Permissions denied. Some features may not work.", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}

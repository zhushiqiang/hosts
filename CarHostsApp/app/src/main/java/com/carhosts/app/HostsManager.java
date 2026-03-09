package com.carhosts.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HostsManager - Manages hosts file for Android 9 car systems
 * Supports programmatic customization of hosts entries
 */
public class HostsManager {
    private static final String TAG = "HostsManager";
    
    // System hosts file path (requires root)
    private static final String SYSTEM_HOSTS_PATH = "/system/etc/hosts";
    
    // Alternative paths for different ROMs
    private static final String[] HOSTS_PATHS = {
        "/system/etc/hosts",
        "/vendor/etc/hosts",
        "/product/etc/hosts",
        "/etc/hosts"
    };
    
    // Backup location in app storage
    private static final String BACKUP_FILENAME = "hosts_backup.txt";
    
    private Context context;
    private Map<String, String> hostsEntries;
    private List<String> comments;
    
    public HostsManager(Context context) {
        this.context = context.getApplicationContext();
        this.hostsEntries = new HashMap<>();
        this.comments = new ArrayList<>();
    }
    
    /**
     * Add or update a hosts entry programmatically
     * @param ipAddress IP address to map
     * @param hostname Hostname to map
     */
    public void addHostEntry(String ipAddress, String hostname) {
        if (ipAddress == null || hostname == null || ipAddress.isEmpty() || hostname.isEmpty()) {
            Log.w(TAG, "Invalid IP or hostname");
            return;
        }
        hostsEntries.put(hostname.trim(), ipAddress.trim());
        Log.d(TAG, "Added host entry: " + hostname + " -> " + ipAddress);
    }
    
    /**
     * Add multiple hosts entries programmatically
     * @param entries Map of hostname -> IP
     */
    public void addHostEntries(Map<String, String> entries) {
        if (entries == null) return;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            addHostEntry(entry.getValue(), entry.getKey());
        }
    }
    
    /**
     * Remove a hosts entry
     * @param hostname Hostname to remove
     */
    public void removeHostEntry(String hostname) {
        if (hostname != null && !hostname.isEmpty()) {
            hostsEntries.remove(hostname.trim());
            Log.d(TAG, "Removed host entry: " + hostname);
        }
    }
    
    /**
     * Clear all custom hosts entries
     */
    public void clearAllEntries() {
        hostsEntries.clear();
        comments.clear();
        Log.d(TAG, "Cleared all host entries");
    }
    
    /**
     * Add a comment line to the hosts file
     * @param comment Comment text
     */
    public void addComment(String comment) {
        if (comment != null && !comment.isEmpty()) {
            comments.add(comment);
        }
    }
    
    /**
     * Check if device has root access
     * @return true if root is available
     */
    public boolean hasRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            Log.e(TAG, "Root check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read current hosts file content
     * @return List of lines from hosts file
     */
    public List<String> readHostsFile() {
        List<String> lines = new ArrayList<>();
        File hostsFile = getHostsFile();
        
        if (!hostsFile.exists()) {
            Log.w(TAG, "Hosts file does not exist");
            return lines;
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(hostsFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading hosts file: " + e.getMessage());
        }
        
        return lines;
    }
    
    /**
     * Apply custom hosts entries to the system hosts file
     * Requires root access on most devices
     * @return true if successful
     */
    public boolean applyHosts() {
        if (!hasRootAccess()) {
            Log.e(TAG, "No root access available");
            return false;
        }
        
        StringBuilder content = new StringBuilder();
        
        // Add header comment
        content.append("# CarHostsApp - Custom hosts configuration\n");
        content.append("# Generated on: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        // Add original hosts entries (read from system)
        List<String> originalLines = readHostsFile();
        for (String line : originalLines) {
            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                // Skip existing entries that we're overriding
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    String existingHostname = parts[1];
                    if (!hostsEntries.containsKey(existingHostname)) {
                        content.append(line).append("\n");
                    }
                } else {
                    content.append(line).append("\n");
                }
            } else if (line.startsWith("#") && !line.contains("CarHostsApp")) {
                content.append(line).append("\n");
            }
        }
        
        content.append("\n# Custom entries by CarHostsApp\n");
        
        // Add custom entries
        for (Map.Entry<String, String> entry : hostsEntries.entrySet()) {
            content.append(entry.getValue()).append("\t").append(entry.getKey()).append("\n");
        }
        
        // Add comments
        for (String comment : comments) {
            content.append("# ").append(comment).append("\n");
        }
        
        // Write to temporary file first
        File tempFile = new File(context.getCacheDir(), "hosts_temp");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(tempFile)))) {
            writer.write(content.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error writing temp file: " + e.getMessage());
            return false;
        }
        
        // Use su to copy to system location
        try {
            File targetFile = getHostsFile();
            
            // Mount system as read-write
            execSuCommand("mount -o rw,remount /system");
            
            // Backup original
            execSuCommand("cp " + targetFile.getAbsolutePath() + " " + 
                         targetFile.getAbsolutePath() + ".bak");
            
            // Copy new hosts file
            execSuCommand("cp " + tempFile.getAbsolutePath() + " " + targetFile.getAbsolutePath());
            
            // Set permissions
            execSuCommand("chmod 644 " + targetFile.getAbsolutePath());
            
            // Remount as read-only
            execSuCommand("mount -o ro,remount /system");
            
            Log.i(TAG, "Hosts file updated successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying hosts: " + e.getMessage());
            return false;
        } finally {
            tempFile.delete();
        }
    }
    
    /**
     * Execute command with root privileges
     */
    private void execSuCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("su");
        process.getOutputStream().write((command + "\n").getBytes());
        process.getOutputStream().write("exit\n".getBytes());
        process.getOutputStream().flush();
        process.waitFor();
    }
    
    /**
     * Get the hosts file location
     */
    private File getHostsFile() {
        for (String path : HOSTS_PATHS) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        return new File(SYSTEM_HOSTS_PATH);
    }
    
    /**
     * Save hosts configuration to app storage (non-root backup)
     */
    public boolean saveToAppStorage() {
        File backupFile = new File(context.getFilesDir(), BACKUP_FILENAME);
        
        StringBuilder content = new StringBuilder();
        content.append("# CarHostsApp - Custom hosts configuration\n");
        content.append("# This is a backup/configuration file\n\n");
        
        for (Map.Entry<String, String> entry : hostsEntries.entrySet()) {
            content.append(entry.getValue()).append("\t").append(entry.getKey()).append("\n");
        }
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(backupFile)))) {
            writer.write(content.toString());
            Log.i(TAG, "Configuration saved to: " + backupFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load hosts configuration from app storage
     */
    public boolean loadFromAppStorage() {
        File backupFile = new File(context.getFilesDir(), BACKUP_FILENAME);
        
        if (!backupFile.exists()) {
            Log.w(TAG, "Backup file does not exist");
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(backupFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    hostsEntries.put(parts[1], parts[0]);
                }
            }
            Log.i(TAG, "Loaded " + hostsEntries.size() + " entries from backup");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error loading configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all current entries
     */
    public Map<String, String> getEntries() {
        return new HashMap<>(hostsEntries);
    }
    
    /**
     * Get count of entries
     */
    public int getEntryCount() {
        return hostsEntries.size();
    }
}

package com.github.carhosts.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.github.carhosts.vpn.util.LogUtils;
import com.github.carhosts.vpn.vservice.CarVpnService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    private static final String PREFS_NAME = "BootPrefs";
    private static final String KEY_ENABLED = "boot_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (getEnabled(context)) {
                LogUtils.i(TAG, "Boot completed, starting VPN service");
                CarVpnService.startVpnService(context);
            }
        }
    }

    public static boolean getEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}

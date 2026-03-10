package com.github.carhosts.vpn.vservice;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarVpnService extends VpnService {
    private static final String TAG = CarVpnService.class.getSimpleName();
    private static final String VPN_ADDRESS = "192.0.2.111";
    private static final String VPN_DNS = "8.8.8.8";

    public static final String ACTION_START = CarVpnService.class.getName() + ".START";
    public static final String ACTION_STOP = CarVpnService.class.getName() + ".STOP";

    private static boolean isRunning = false;
    private ParcelFileDescriptor vpnInterface = null;
    private ExecutorService executorService;
    private Selector udpSelector;
    private Selector tcpSelector;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "VPN Service Created");
        setupVPN();
        if (vpnInterface == null) {
            Log.e(TAG, "Failed to create VPN interface");
            stopSelf();
            return;
        }
        isRunning = true;
        startPacketProcessing();
    }

    private void setupVPN() {
        Builder builder = new Builder();
        builder.addAddress(VPN_ADDRESS, 32);
        builder.addRoute(VPN_DNS, 32);
        builder.addDnsServer(VPN_DNS);
        builder.setSession("CarHosts VPN");
        
        // Add common car app packages to bypass VPN if needed
        try {
            builder.addDisallowedApplication("com.google.android.apps.maps");
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Package not found: " + e.getMessage());
        }
        
        vpnInterface = builder.establish();
    }

    private void startPacketProcessing() {
        try {
            udpSelector = Selector.open();
            tcpSelector = Selector.open();
            executorService = Executors.newFixedThreadPool(4);
            
            ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue = new ConcurrentLinkedQueue<>();
            
            executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
            executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, networkToDeviceQueue, udpSelector, this));
            executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector));
            executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));
            executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(),
                    deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
            
            Log.i(TAG, "Packet processing started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting packet processing", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_STOP.equals(intent.getAction())) {
                stopVpn();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static void startVpnService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.e(TAG, "VPN permission required");
        } else {
            context.startService(new Intent(context, CarVpnService.class).setAction(ACTION_START));
        }
    }

    public static void stopVpnService(Context context) {
        context.startService(new Intent(context, CarVpnService.class).setAction(ACTION_STOP));
    }

    private void stopVpn() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        isRunning = false;
        closeResources(udpSelector, tcpSelector, vpnInterface);
        stopSelf();
        Log.i(TAG, "VPN Service Stopped");
    }

    @Override
    public void onRevoke() {
        stopVpn();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                if (resource != null) resource.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing resource", e);
            }
        }
    }
}

package com.github.carhosts.vpn.vservice;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPInput implements Runnable {
    private static final String TAG = TCPInput.class.getSimpleName();
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;

    public TCPInput(ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue, Selector selector) {
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        Log.i(TAG, "TCP Input started");
        try {
            while (!Thread.interrupted()) {
                if (selector.select(1000) > 0) {
                    // Process TCP packets
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "TCP Input error", e);
        }
        Log.i(TAG, "TCP Input stopped");
    }
}

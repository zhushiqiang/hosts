package com.github.carhosts.vpn.vservice;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UDPInput implements Runnable {
    private static final String TAG = UDPInput.class.getSimpleName();
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;

    public UDPInput(ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue, Selector selector) {
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        Log.i(TAG, "UDP Input started");
        try {
            while (!Thread.interrupted()) {
                if (selector.select(1000) > 0) {
                    // Process UDP packets
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "UDP Input error", e);
        }
        Log.i(TAG, "UDP Input stopped");
    }
}

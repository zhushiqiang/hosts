package com.github.carhosts.vpn.vservice;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPOutput implements Runnable {
    private static final String TAG = TCPOutput.class.getSimpleName();
    private ConcurrentLinkedQueue<Packet> deviceToNetworkQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;
    private Context context;

    public TCPOutput(ConcurrentLinkedQueue<Packet> deviceToNetworkQueue,
                     ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue,
                     Selector selector, Context context) {
        this.deviceToNetworkQueue = deviceToNetworkQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
        this.context = context;
    }

    @Override
    public void run() {
        Log.i(TAG, "TCP Output started");
        try {
            while (!Thread.interrupted()) {
                Packet packet = deviceToNetworkQueue.poll();
                if (packet != null) {
                    // TCP DNS handling can be added here
                    ByteBufferPool.release(packet.backingBuffer);
                }
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "TCP Output interrupted");
        } catch (Exception e) {
            Log.e(TAG, "TCP Output error", e);
        }
        Log.i(TAG, "TCP Output stopped");
    }
}

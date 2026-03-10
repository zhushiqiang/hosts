package com.github.carhosts.vpn.vservice;

import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VPNRunnable implements Runnable {
    private static final String TAG = VPNRunnable.class.getSimpleName();
    private FileDescriptor vpnFileDescriptor;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

    public VPNRunnable(FileDescriptor vpnFileDescriptor,
                       ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                       ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                       ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue) {
        this.vpnFileDescriptor = vpnFileDescriptor;
        this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
        this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
    }

    @Override
    public void run() {
        Log.i(TAG, "VPN Runnable started");
        FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
        FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();
        
        try {
            ByteBuffer bufferToNetwork = null;
            boolean dataSent = true;
            
            while (!Thread.interrupted()) {
                if (dataSent) {
                    bufferToNetwork = ByteBufferPool.acquire();
                } else {
                    bufferToNetwork.clear();
                }

                int readBytes = vpnInput.read(bufferToNetwork);
                if (readBytes > 0) {
                    dataSent = true;
                    bufferToNetwork.flip();
                    Packet packet = new Packet(bufferToNetwork);
                    if (packet.isUDP()) {
                        deviceToNetworkUDPQueue.offer(packet);
                    } else if (packet.isTCP()) {
                        deviceToNetworkTCPQueue.offer(packet);
                    } else {
                        Log.w(TAG, "Unknown packet type");
                        dataSent = false;
                        ByteBufferPool.release(bufferToNetwork);
                    }
                } else {
                    dataSent = false;
                }

                ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                if (bufferFromNetwork != null) {
                    bufferFromNetwork.flip();
                    while (bufferFromNetwork.hasRemaining()) {
                        vpnOutput.write(bufferFromNetwork);
                    }
                    ByteBufferPool.release(bufferFromNetwork);
                }
                
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "VPN Runnable interrupted");
        } catch (Exception e) {
            Log.e(TAG, "VPN Runnable error", e);
        } finally {
            try {
                vpnInput.close();
                vpnOutput.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN channels", e);
            }
        }
        Log.i(TAG, "VPN Runnable stopped");
    }
}

package com.github.carhosts.vpn.vservice;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UDPOutput implements Runnable {
    private static final String TAG = UDPOutput.class.getSimpleName();
    private ConcurrentLinkedQueue<Packet> deviceToNetworkQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;
    private Context context;

    public UDPOutput(ConcurrentLinkedQueue<Packet> deviceToNetworkQueue,
                     ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue,
                     Selector selector, Context context) {
        this.deviceToNetworkQueue = deviceToNetworkQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
        this.context = context;
    }

    @Override
    public void run() {
        Log.i(TAG, "UDP Output started");
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1000);
            
            while (!Thread.interrupted()) {
                Packet packet = deviceToNetworkQueue.poll();
                if (packet != null) {
                    ByteBuffer buffer = packet.backingBuffer;
                    
                    // Check if this is a DNS query (port 53)
                    if (packet.getDestinationPort() == 53) {
                        ByteBuffer response = DnsChange.handleDnsPacket(packet);
                        if (response != null) {
                            networkToDeviceQueue.offer(response);
                            continue;
                        }
                    }
                    
                    // Forward to real DNS server
                    try {
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        InetAddress dnsServer = InetAddress.getByName("8.8.8.8");
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, dnsServer, 53);
                        socket.send(sendPacket);
                        
                        // Receive response
                        byte[] receiveData = new byte[512];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);
                        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
                        receiveBuffer.put(receivePacket.getData(), 0, receivePacket.getLength());
                        networkToDeviceQueue.offer(receiveBuffer);
                    } catch (Exception e) {
                        Log.e(TAG, "DNS forward error", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "UDP Output error", e);
        }
        Log.i(TAG, "UDP Output stopped");
    }
}

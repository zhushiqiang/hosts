package com.github.carhosts.vpn.vservice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet {
    public ByteBuffer backingBuffer;
    public int ipOffset;
    public int ipHeaderLength;
    public int transportHeaderLength;
    private int totalLength;
    private int sourcePort;
    private int destinationPort;

    public Packet(ByteBuffer packetBuffer) {
        this.backingBuffer = packetBuffer;
        parsePacket();
    }

    private void parsePacket() {
        byte version = backingBuffer.get(backingBuffer.position());
        if ((version & 0xF0) == 0x40) {
            ipOffset = backingBuffer.position();
            ipHeaderLength = (version & 0x0F) * 4;
            totalLength = getShort(ipOffset + 2);
            
            byte protocol = backingBuffer.get(ipOffset + 9);
            if (protocol == 17 || protocol == 6) {
                transportHeaderLength = 8;
                sourcePort = getShort(ipOffset + ipHeaderLength);
                destinationPort = getShort(ipOffset + ipHeaderLength + 2);
            }
        }
    }

    public boolean isUDP() {
        return getProtocol() == 17;
    }

    public boolean isTCP() {
        return getProtocol() == 6;
    }

    private byte getProtocol() {
        if (ipOffset >= 0 && ipOffset + 9 < backingBuffer.limit()) {
            return backingBuffer.get(ipOffset + 9);
        }
        return 0;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void swapSourceAndDestination() {
        // Implementation for swapping addresses
    }

    public void updateUDPBuffer(ByteBuffer newBuffer, int length) {
        this.backingBuffer = newBuffer;
        this.totalLength = length;
    }

    private short getShort(int offset) {
        if (offset + 1 >= backingBuffer.limit()) {
            return 0;
        }
        return (short) (((backingBuffer.get(offset) & 0xFF) << 8) | 
                       (backingBuffer.get(offset + 1) & 0xFF));
    }
}

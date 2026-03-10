package com.github.carhosts.vpn.vservice;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ByteBufferPool {
    private static final int POOL_SIZE = 256;
    private static final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();
    private static final int BUFFER_SIZE = 16384;

    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        }
    }

    public static ByteBuffer acquire() {
        ByteBuffer buffer = pool.poll();
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        }
        return buffer;
    }

    public static void release(ByteBuffer buffer) {
        if (buffer != null) {
            buffer.clear();
            pool.offer(buffer);
        }
    }

    public static void clear() {
        pool.clear();
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(ByteBuffer.allocateDirect(BUFFER_SIZE));
        }
    }
}

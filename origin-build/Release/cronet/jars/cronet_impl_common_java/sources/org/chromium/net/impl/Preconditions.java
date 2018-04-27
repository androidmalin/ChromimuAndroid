package org.chromium.net.impl;

import java.nio.ByteBuffer;

public final class Preconditions {
    private Preconditions() {
    }

    static void checkDirect(ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("byteBuffer must be a direct ByteBuffer.");
        }
    }

    static void checkHasRemaining(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("ByteBuffer is already full.");
        }
    }
}

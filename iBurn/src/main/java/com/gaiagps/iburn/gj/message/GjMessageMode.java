package com.gaiagps.iburn.gj.message;

import java.nio.ByteBuffer;

public class GjMessageMode extends GjMessage {
    protected byte mode = Mode.Buffered.getValue();

    public GjMessageMode(Mode mode) {
        super(Type.Mode);
        this.mode = mode.getValue();
    }

    public byte[] toByteArray() {
        // preamble + type + length + data + checksum
        int length = preamble.length + 1 + 1 + 1 + 1;

        ByteBuffer buffer = ByteBuffer.allocate(length);

        buffer.put(preamble);
        buffer.put(type);
        buffer.put((byte)1);
        buffer.put(mode);
        buffer.put(checksum(buffer));

        return buffer.array();
    }

    @Override
    public String toString() {
        return Type.Mode + ":" + Mode.valueOf(mode) ;
    }
}

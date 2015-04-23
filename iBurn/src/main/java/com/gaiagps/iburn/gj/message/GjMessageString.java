package com.gaiagps.iburn.gj.message;

import java.nio.ByteBuffer;

public class GjMessageString extends GjMessage {
    protected byte[] data = new byte[0];

    public GjMessageString(Type type, String dataString) {
        super(type);
        // force 140
        int length = Math.min(dataString.length(), 140);
        data = dataString.substring(0, length).getBytes();
    }

    public byte[] toByteArray() {
        // preamble + type + length + data + checksum
        int length = preamble.length + 1 + 1 + data.length + 1;

        ByteBuffer buffer = ByteBuffer.allocate(length);

        buffer.put(preamble);
        buffer.put(type);
        buffer.put((byte) data.length);
        buffer.put(data);
        buffer.put(checksum(buffer));

        return buffer.array();
    }

    @Override
    public String toString() {
        return Type.valueOf(type) + ":" + new String(data) ;
    }
}

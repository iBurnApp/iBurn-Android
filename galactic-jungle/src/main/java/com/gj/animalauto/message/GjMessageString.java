package com.gj.animalauto.message;

public class GjMessageString extends GjMessage {

    public static final int MAX_LENGTH = 140;

    public GjMessageString(Type type, String dataString) {
        super(type);
        setString(dataString);
    }

    public GjMessageString(Type type, byte packetNumber, byte vehicle, byte[] data) {
        super(type, packetNumber, vehicle);
        setData(data);
    }

    public String getString() {
        return new String(data);
    }

    private void setString(String string) {
        // force 140
        int length = Math.min(string.length(), MAX_LENGTH);
        data = string.substring(0, length).getBytes();
    }

    @Override
    public String toString() {
        return super.toString() + ":" + new String(data);
    }

}

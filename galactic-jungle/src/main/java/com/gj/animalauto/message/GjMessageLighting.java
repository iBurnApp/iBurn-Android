package com.gj.animalauto.message;

public class GjMessageLighting extends GjMessageString {

    public GjMessageLighting(String dataString) {
        super(Type.Lighting, dataString);
    }

    public GjMessageLighting(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Lighting, packetNumber, vehicle, data);
    }
}

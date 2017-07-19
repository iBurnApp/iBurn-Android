package com.gj.animalauto.message;

public class GjMessageResponse extends GjMessage {

    public GjMessageResponse(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Response, packetNumber, vehicle);
        setByte(data[0]); // true == checksum OK
    }

    public boolean isOK() {
        return getBoolean();
    }

    private String checksumStatus() {
        return isOK() ? "No error" : "Checksum Error. packet " + getPacketNumber();
    }

    @Override
    public String toString() {
        return super.toString()+": " + checksumStatus();
    }
}

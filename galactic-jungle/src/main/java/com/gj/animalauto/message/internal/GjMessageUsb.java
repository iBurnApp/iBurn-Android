package com.gj.animalauto.message.internal;

import com.gj.animalauto.message.GjMessage;

public class GjMessageUsb extends GjMessage {

    public GjMessageUsb(boolean attached) {
        super(Type.USB);
        setByte(attached);
    }

    public GjMessageUsb(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.USB, packetNumber, vehicle);
        setByte(data[0]);
    }

    public boolean getStatus() {
        return getBoolean();
    }

    public String getStatusString() {
        return getStatus() ? "Attached" : "Detached";
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getStatusString() ;
    }
}

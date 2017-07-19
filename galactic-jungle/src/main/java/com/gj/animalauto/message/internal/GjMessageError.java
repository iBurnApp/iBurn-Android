package com.gj.animalauto.message.internal;

import com.gj.animalauto.message.GjMessageString;

public class GjMessageError extends GjMessageString {

    public GjMessageError(String dataString) {
        super(Type.Error, dataString);
    }

    public GjMessageError(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Error, packetNumber, vehicle, data);
    }
}

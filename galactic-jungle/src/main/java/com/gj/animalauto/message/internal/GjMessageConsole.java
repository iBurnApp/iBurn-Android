package com.gj.animalauto.message.internal;

import com.gj.animalauto.message.GjMessageString;

public class GjMessageConsole extends GjMessageString {

    public GjMessageConsole(String dataString) {
        super(Type.Console, dataString);
    }

    public GjMessageConsole(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Console, packetNumber, vehicle, data);
    }
}

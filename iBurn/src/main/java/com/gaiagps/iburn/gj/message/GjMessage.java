package com.gaiagps.iburn.gj.message;

import com.google.android.gms.maps.model.LatLng;

import java.nio.ByteBuffer;

/**
 * Created by liorsaar on 2015-04-21
 */

/*
    Preamble:
    All message begin with 0xFF 0x55 0xAA

    Message Type:
    0x01 StatusRequest request
    0x02 Mode
    0x03 Report own GPS location
    0x04 Request buffered GPS locations
    0x05 Lighting cues
    0x06 Text message

    Data length:
    Can be any number between 0 and 140 (0x00 - 0x8C)
    For [StatusRequest request] use length of 0
    For [Mode] use length 1

    Data:
    The length of this data must match the [Data length]
    The GPS, Lighting cue and Text message formats are somewhat arbitrary, whatever format you choose will be the format returned to your app.
    For [StatusRequest request] send no data bytes
    For [Mode]:
    Send a byte 0x00 for standard buffered mode. This will buffer GPS updates until they are requested.
    Send a byte 0x01 for non-buffered mode.  This will report GPS updates as they are received.

    Checksum:
    This will be a single byte sum (truncated to the LSB) that includes ALL bytes in the packet including the preamble. If an incorrect checksum is received an error packet will be returned

    Sample packet:
    0xFF 0x55 0xAA 0x03 0x02 0x48 0x49 0x94
    This is a text message sending "HI".
 */

public class GjMessage {
    public static final byte[] preamble = {(byte) 0xFF, (byte) 0x55, (byte) 0xAA};
    protected byte type;

    public GjMessage(Type type) {
        this.type = type.getValue();
    }

    @Override
    public String toString() {
        return Type.valueOf(type).toString() ;
    }

    public String toHexString() {
        StringBuffer sb = new StringBuffer();
        for (byte b : toByteArray()) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

    public byte[] toByteArray() {
        // preamble + type + length + data + checksum
        int length = preamble.length + 1 + 0 + 0 + 1;

        ByteBuffer buffer = ByteBuffer.allocate(length);

        buffer.put(preamble);
        buffer.put(type);
        buffer.put(checksum(buffer));

        return buffer.array();
    }

    protected byte checksum(ByteBuffer byteBuffer) {
        int checksum = 0;

        for (int i : byteBuffer.array()) {
            checksum += i;
        }
        return (byte) checksum;
    }

    public static enum Type {
        StatusRequest((byte) 0x01),
        Mode((byte) 0x02),
        ReportGps((byte) 0x03),
        RequestGps((byte) 0x04),
        Lighting((byte) 0x05),
        Text((byte) 0x06);

        private final byte value;

        Type(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static Type valueOf(byte b) {
            if (b-1 >= values().length)
                throw new RuntimeException("Type: Illegal Value: " + b);
            return values()[b-1];
        }
    }

    public static enum Mode {
        // standard buffered mode. This will buffer GPS updates until they are requested.
        Buffered((byte) 0x00),
        // non-buffered mode.  This will report GPS updates as they are received.
        NonBuffered((byte) 0x01);

        private final byte value;

        Mode(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static Mode valueOf(byte b) {
            if (b >= values().length)
                throw new RuntimeException("Mode: Illegal Value: " + b);
            return values()[b];
        }
    }

    public static void test() {

        GjMessageText textMessage = new GjMessageText("HI");
        byte[] buf = textMessage.toByteArray();
        String hex = textMessage.toHexString();
        String s = textMessage.toString();

        GjMessageStatusRequest statusRequestMessage = new GjMessageStatusRequest();
        buf = statusRequestMessage.toByteArray();
        hex = statusRequestMessage.toHexString();
        s = statusRequestMessage.toString();

        GjMessageMode modeMessage = new GjMessageMode(GjMessage.Mode.Buffered);
        buf = modeMessage.toByteArray();
        hex = modeMessage.toHexString();
        s = modeMessage.toString();

        modeMessage = new GjMessageMode(GjMessage.Mode.NonBuffered);
        buf = modeMessage.toByteArray();
        hex = modeMessage.toHexString();
        s = modeMessage.toString();

        LatLng latLng = new LatLng(40.7888, -119.20315);
        GjMessageReportGps reportGpsMessage = new GjMessageReportGps(latLng);
        buf = reportGpsMessage.toByteArray();
        hex = reportGpsMessage.toHexString();
        s = reportGpsMessage.toString();

    }

}


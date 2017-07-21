package com.gj.animalauto.message;

public class GjMessageStatusResponse extends GjMessage {

    private static final byte BITMASK_RADIO = 0x01;
    private static final byte BITMASK_VOLTAGE = 0x02;
    private static final byte BITMASK_TEMP = 0x04;
    private static final byte BITMASK_COMPASS = 0x08;
    private static final byte BITMASK_GPS = 0x10;

    public GjMessageStatusResponse(byte status) { // used for testing only
        super(Type.StatusResponse);
        setByte(status);
    }

    public GjMessageStatusResponse(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.StatusResponse, packetNumber, vehicle);
        setByte(data[0]);
    }

    public boolean getErrorRadio() { return (getByte() & BITMASK_RADIO) != 0; }
    public boolean getErrorVoltage() { return false;} //(getByte() & BITMASK_VOLTAGE) != 0; }
    public boolean getErrorTemp() { return (getByte() & BITMASK_TEMP) != 0; }
    public boolean getErrorCompass() { return (getByte() & BITMASK_COMPASS) != 0; }
    public boolean getErrorGps() { return (getByte() & BITMASK_GPS) != 0; }
    public boolean isCriticalError() { return getErrorCompass() | getErrorGps() | getErrorRadio() | getErrorTemp() | getErrorVoltage(); }

    public static byte createData(boolean radio, boolean voltage, boolean temp, boolean compass, boolean gps) {
        byte data = 0x00;
        if (radio) data &= BITMASK_RADIO;
        if (voltage) data &= BITMASK_VOLTAGE;
        if (temp) data &= BITMASK_TEMP;
        if (compass) data &= BITMASK_COMPASS;
        if (gps) data &= BITMASK_GPS;
        return data;
    }
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString()).append(": ");
        sb.append("Vehicle:" + getVehicle() +", ");
        sb.append("Packet:" + getPacketNumber() + ", ");
        sb.append("Status: 0x" + toHexString(data));
        return sb.toString();
    }
}

package com.gj.animalauto.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GjMessageGps extends GjMessage {

    private long time;
    private double lat;
    private double lng;
    private double head;

    public GjMessageGps(byte[] data) {
        super(Type.Gps);
        setData(data);
    }

    public GjMessageGps(byte packetNumber, byte vehicle, byte[] data) {
        super(Type.Gps, packetNumber, vehicle);
        setData(data);
    }

    @Override
    protected void setData(byte[] data) {
        super.setData(data);
        time = ByteBuffer.wrap(data,  0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        lat  = ByteBuffer.wrap(data,  4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.0000001;
        lng  = ByteBuffer.wrap(data,  8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.0000001;
        head = ByteBuffer.wrap(data, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()*0.01;
    }

    public static ByteBuffer createData(int time, double lat, double lng, double head) {
        ByteBuffer data = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(time);
        data.putInt((int) (lat * 10000000));
        data.putInt((int) (lng * 10000000));
        data.putInt((int) (head * 100));
        return data;
    }

    public double getHead() {
        return head;
    }

    public long getTime() {
        return time;
    }

    public double getLat() {
        return lat;
    }

    public double getLong() {
        return lng;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString()).append(": ");
        sb.append("Vehicle:" + getVehicle() + ", ");
        sb.append("time:" + getTime() + ", ");
        sb.append("lat:" + getLat() + ", ");
        sb.append("long:" + getLong() + ", ");
        sb.append("head:" + getHead());
        return sb.toString();
    }
}

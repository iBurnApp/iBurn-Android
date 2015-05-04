package com.gaiagps.iburn.gj.ftdi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import org.apache.http.util.ByteArrayBuffer;

import java.io.PrintWriter;
import java.io.StringWriter;

public class FtdiService extends Service {
    private final static String TAG = "FtdiService";
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private static D2xxManager ftD2xx = null;
    private FT_Device ftDev;

    boolean mThreadIsStopped = true;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        FtdiService getService() {
            return FtdiService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        poolInit();

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG, ex.toString());
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThreadIsStopped = true;
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadcastMessage("onStartCommand: " + START_STICKY);
        return START_STICKY;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            broadcastMessage("action: "+action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // never come here(when attached, go to onNewIntent)
                openDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                closeDevice();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        return mBinder;
    }

    final int POOL_SIZE = 2;
    final int POOL_BUFFER_SIZE = 4096;
    ByteArrayBuffer[] pool;
    int poolIndex = 0;

    private void poolInit() {
        pool = new ByteArrayBuffer[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE ; i++) {
            pool[i] = new ByteArrayBuffer(POOL_BUFFER_SIZE);
        }
    }

    public int read(byte[] bytes) {
        broadcastMessage("read");
        try {
            if (ftDev == null) {
                openDevice();
                if(ftDev == null) {
                    broadcastError( "FTDI device not found");
                    return -1;
                }
            }

            synchronized (ftDev) {
                ByteArrayBuffer bab = pool[poolIndex];
                int length = bab.length();
                if (length > bytes.length) {
                    length = bytes.length;
                }
                for (int i = 0 ; i < length ; i++) {
                    bytes[i] = (byte)bab.byteAt(i);
                }
                // advance pool index
                if (++poolIndex >= POOL_SIZE) {
                    poolIndex = 0;
                }
                bab.clear();
                return length;
            }
        } catch (Throwable t) {
            broadcastException(bytes, t);
            return -1;
        }
    }

    private void broadcastException(byte[] bytes, Throwable t) {
        broadcastError("Exception: " + t.getMessage());
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        byte[] stack = stringWriter.toString().getBytes();
        int length = stack.length;
        for (int i = 0 ; i < length ; i++) {
            bytes[i] = stack[i];
        }
        broadcastError("ERROR:" + stringWriter.toString());
    }

    public int send(byte[] bytes) {
        broadcastMessage("send:" + new String(bytes));
        if (ftDev == null) {
            openDevice();
        }
        int written = write(bytes);
        return written;
    }

    private void broadcast(byte[] bytes) {
        Intent intent = new Intent("com.ksksue.app.ftdi_uart.xxx");
        intent.putExtra("bytes", bytes);
        sendBroadcast(intent);
    }

    private void broadcastError(String string) {
        Intent intent = new Intent("com.ksksue.app.ftdi_uart.xxx");
        intent.putExtra("error", string);
        sendBroadcast(intent);
    }

    private void broadcastMessage(String string) {
        Intent intent = new Intent("com.ksksue.app.ftdi_uart.xxx");
        intent.putExtra("message", string);
        sendBroadcast(intent);
    }

    public int write(byte[] bytes) {
        if(ftDev == null) {
            broadcastError( "FTDI device not found");
            return -1;
        }

        synchronized (ftDev) {
            if(ftDev.isOpen() == false) {
                broadcastError( "Device not open");
                Log.e(TAG, "onClickWrite : Device is not open");
                return -1;
            }

            ftDev.setLatencyTimer((byte) 16);
            int written = ftDev.write(bytes, bytes.length);
            return written;
        }
    }

    private void openDevice() {
        if(ftDev != null) {
            if(ftDev.isOpen()) {
                if(mThreadIsStopped) {
                    SetConfig(9600, (byte)8, (byte)1, (byte)0, (byte)3);
                    ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                    ftDev.restartInTask();
                    new Thread(mLoop).start();
                }
                return;
            }
        }

        int devCount = 0;
        devCount = ftD2xx.createDeviceInfoList(this);
        broadcastMessage("devCount: " + devCount);
        if(devCount <= 0) {
            broadcastError("No Devices found: " + devCount);
            return;
        }

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);

        for( int i = 0; i < deviceList.length; i++) {
            D2xxManager.FtDeviceInfoListNode node = deviceList[i];
            String info = "#" + i + ", id:"+ node.id + ", serial:" + node.serialNumber + ", desc:" + node.description;
            broadcastMessage(info);
        }

        int deviceIndex = 0; // FIXME - must identify the right device

        if(ftDev == null) {
            ftDev = ftD2xx.openByIndex(this, deviceIndex);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(this, deviceIndex);
            }
        }

        if(ftDev.isOpen()) {
            if(mThreadIsStopped) {
                SetConfig(9600, (byte)8, (byte)1, (byte)0, (byte)3);
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                new Thread(mLoop).start();
            }
        }
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            int i;
            int readSize;
            mThreadIsStopped = false;
            while(true) {
                if(mThreadIsStopped) {
                    break;
                }

                synchronized (ftDev) {
                    readSize = ftDev.getQueueStatus();
                    if(readSize>0) {
                        byte[] inputBytes = new byte[readSize];
                        ftDev.read(inputBytes, readSize);
                        ByteArrayBuffer bab = pool[poolIndex];
                        bab.append(inputBytes,0, readSize);
                    } // end of if(readSize>0)
                } // end of synchronized

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }

            }
        }
    };

    private void closeDevice() {
        mThreadIsStopped = true;
        if(ftDev != null) {
            ftDev.close();
        }
    }
    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (ftDev.isOpen() == false) {
            Log.e(TAG, "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
//        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x11, (byte) 0x13);
    }

}
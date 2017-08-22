package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.WifiUtilKt;
import com.gj.animalauto.OscClient;
import com.gj.animalauto.OscHostManager;
import com.gj.animalauto.PrefsHelper;
import com.gj.animalauto.wifi.WifiManager;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

import io.reactivex.disposables.Disposable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import timber.log.Timber;

/**
 * Created by liorsaar on 4/19/15
 */

/*
a. Effect Mode - grid of buttons (2x4 for now - can be changed later)
Brightness - slider
Saturation - slider
Hue shift(color) - slider with color button
Effect speed - slider
Color Palette - drop down menu with bitmap for every palette
Effect parameter 1 - slider
Effect parameter 2 - slider
 */
public class GjLightingFragment extends Fragment implements Function1<OscHostManager.OscHost, Unit> {
    private static final String TAG = "GjLightingFragment";
    private static final int LIGHTING_MESSAGE_REFRESH_RATE = 5; // seconds
    private static SeekBar[] seekBar;

    private long seekbarTimeOfLastUpdate = 0;

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // throttle the sliders to 30 messages per second
            if (oscClient == null) {
                Timber.w("Cannot send command, OSC client not ready");
                return;
            }

            if (isOkToSendNextMessage()) {
//                onChange();
                float value = progress / 100f;
                int id = seekBar.getId();
                switch (id) {
                    case R.id.lightBrightness:
                        oscClient.setBrightness(value);
                        break;
                    case R.id.lightHue:
                        oscClient.setHue(value);
                        break;
                    case R.id.lightSpeed:
                        oscClient.setSpeed(value);
                        break;
//                    case R.id.lightDensity:
//                        oscClient.setDensity(value);
//                        break;

                    default:
                        Timber.w("Unknown slider toggled!");

                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private class ButtonOnTouchListener implements View.OnTouchListener {

        private int buttonNumber;

        public ButtonOnTouchListener(int buttonNumber) {
            this.buttonNumber = buttonNumber;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            if (oscClient == null) {
                Timber.w("Cannot send command, OSC client not ready");
                return false;
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                oscClient.sendButtonPress(buttonNumber);
                return true;
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                oscClient.sendButtonPressReleased(buttonNumber);
                return true;
            }
            return false;
        }
    }

    private OscHostManager oscHostManager;
    private OscClient oscClient;
    private WifiManager wifiManager;
    private Disposable wifiConnectDisposable;

    public static GjLightingFragment newInstance() {
        return new GjLightingFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context appCtx = getActivity().getApplicationContext();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lighting, container, false);

        seekBar = new SeekBar[3]; //4];
        seekBar[0] = view.findViewById(R.id.lightBrightness);
        seekBar[1] = view.findViewById(R.id.lightHue);
        seekBar[2] = view.findViewById(R.id.lightSpeed);
//        seekBar[3] = view.findViewById(R.id.lightDensity);

        for (int i = 0; i < seekBar.length; i++) {
            seekBar[i].setOnSeekBarChangeListener(seekBarChangeListener);
        }

        view.findViewById(R.id.presetBtn1).setOnTouchListener(new ButtonOnTouchListener(1));
        view.findViewById(R.id.presetBtn2).setOnTouchListener(new ButtonOnTouchListener(2));
        view.findViewById(R.id.presetBtn3).setOnTouchListener(new ButtonOnTouchListener(3));
        view.findViewById(R.id.presetBtn4).setOnTouchListener(new ButtonOnTouchListener(4));
        view.findViewById(R.id.presetBtn5).setOnTouchListener(new ButtonOnTouchListener(5));
        view.findViewById(R.id.presetBtn6).setOnTouchListener(new ButtonOnTouchListener(6));
        view.findViewById(R.id.presetBtn7).setOnTouchListener(new ButtonOnTouchListener(7));
        view.findViewById(R.id.presetBtn8).setOnTouchListener(new ButtonOnTouchListener(8));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (wifiManager == null) {
            wifiManager = WifiManager.getSharedInstance(getContext());
        }

        wifiManager.start();

        PrefsHelper gjPrefs = new PrefsHelper(getActivity().getApplicationContext());

        if (TextUtils.isEmpty(gjPrefs.getOscWifiSsid()) ||
                TextUtils.isEmpty(gjPrefs.getOscWifiPass())) {

            WifiUtilKt.showWifiCredentialsDialog(getActivity(), (ssid, password) -> {
                gjPrefs.setOscWifiSsid(ssid);
                gjPrefs.setOscWifiPass(password);

                beginOscWifiConnection(ssid, password);
            });

        } else {
            beginOscWifiConnection(gjPrefs.getOscWifiSsid(), gjPrefs.getOscWifiPass());
        }
    }

    private void beginOscWifiConnection(String wifiSsid, String wifiPass) {
        wifiConnectDisposable = wifiManager.connectToWpaSsid(wifiSsid, wifiPass)
                .filter(connection -> connection.connected && connection.ssid.equals(wifiSsid))
                .firstElement()
                .subscribe(wiFiConnection -> {
                            Timber.d("%s %s", wiFiConnection.connected ? "connected to " : "disconnected from ",
                                    wiFiConnection.ssid);


                            if (wiFiConnection.connected && wiFiConnection.ssid.equals(wifiSsid)) {
                                if (oscHostManager == null) {
                                    oscHostManager = new OscHostManager(getActivity().getApplicationContext());
//                                    oscHostManager = new OscMdnsManager(getActivity().getApplicationContext(), oscClient.Companion.getDefaultLocalPort());
                                }

                                // If no primary host set, will show host selection dialog
                                oscHostManager.startDiscovery(getActivity(), this);
                            }
                        }

                );
    }

    @Override
    public void onStop() {
        super.onStop();

        if (wifiManager != null) {
            wifiManager.release();
            // wifiManager handles gracefully re-creating released resources
        }

        if (oscHostManager != null) {
            oscHostManager.stopDiscovery();
            oscHostManager = null;
        }

        if (oscClient != null) {
            oscClient.release();
            oscClient = null;
        }

        if (wifiConnectDisposable != null) {
            wifiConnectDisposable.dispose();
            wifiConnectDisposable = null;
        }
    }

    private boolean isOkToSendNextMessage() {
        if (System.currentTimeMillis() - seekbarTimeOfLastUpdate > 200) {
            seekbarTimeOfLastUpdate = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    // OSC Host selection callback

    @Override
    public Unit invoke(OscHostManager.OscHost oscHost) {
        Timber.d("Setting primary OSC Host %s", oscHost.getHostname());
        oscHostManager.setPrimaryOscHost(oscHost.getHostname());
        connectToOscHost(oscHost.getHostname(), oscHost.getAddress(), oscHost.getPort());
        return Unit.INSTANCE;
    }

    private void connectToOscHost(@NotNull String hostName, @NotNull InetAddress hostAddress, int hostPort) {
        if (oscClient == null) {
            Timber.d("Connecting to OSC client %s", hostName);
            oscClient = new OscClient(hostAddress, hostPort);
            oscClient.listen();
        } else {
            Timber.w("Already connected to an OSC Host, ignoring request");
        }
    }
}

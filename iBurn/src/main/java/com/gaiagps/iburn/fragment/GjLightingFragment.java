package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.gaiagps.iburn.R;
import com.gj.animalauto.OscClient;
import com.gj.animalauto.OscHostDiscoveryDialog;
import com.gj.animalauto.OscMdnsManager;
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
public class GjLightingFragment extends Fragment implements OscMdnsManager.Callback, Function1<OscHostDiscoveryDialog.OscHost, Unit> {
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
                    case R.id.lightSaturation:
                        oscClient.setSaturation(value);
                        break;
                    case R.id.lightSpeed:
                        oscClient.setSpeed(value);
                        break;
                    case R.id.lightDensity:
                        oscClient.setDensity(value);
                        break;

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

    private class ButtonOnClickListener implements View.OnClickListener {

        private int buttonNumber;

        public ButtonOnClickListener(int buttonNumber) {
            this.buttonNumber = buttonNumber;
        }

        @Override
        public void onClick(View view) {
            if (oscClient == null) {
                Timber.w("Cannot send command, OSC client not ready");
                return;
            }

            if (isOkToSendNextMessage()) {
                oscClient.sendButtonPress(buttonNumber);
            }
        }
    }

    private PrefsHelper gjPrefs;
    private OscMdnsManager oscMdnsManager;
    private OscClient oscClient;
    private WifiManager wifiManager;
    private OscHostDiscoveryDialog dialogHelper;
    private Disposable wifiConnectDisposable;

    public static GjLightingFragment newInstance() {
        return new GjLightingFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context appCtx = getActivity().getApplicationContext();
        gjPrefs = new PrefsHelper(appCtx);
        dialogHelper = new OscHostDiscoveryDialog(appCtx);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lighting, container, false);

        seekBar = new SeekBar[4];
        seekBar[0] = view.findViewById(R.id.lightBrightness);
        seekBar[1] = view.findViewById(R.id.lightSaturation);
        seekBar[2] = view.findViewById(R.id.lightSpeed);
        seekBar[3] = view.findViewById(R.id.lightDensity);

        for (int i = 0; i < seekBar.length; i++) {
            seekBar[i].setOnSeekBarChangeListener(seekBarChangeListener);
        }

        view.findViewById(R.id.presetBtn1).setOnClickListener(new ButtonOnClickListener(1));
        view.findViewById(R.id.presetBtn2).setOnClickListener(new ButtonOnClickListener(2));
        view.findViewById(R.id.presetBtn3).setOnClickListener(new ButtonOnClickListener(3));
        view.findViewById(R.id.presetBtn4).setOnClickListener(new ButtonOnClickListener(4));
        view.findViewById(R.id.presetBtn5).setOnClickListener(new ButtonOnClickListener(5));
        view.findViewById(R.id.presetBtn6).setOnClickListener(new ButtonOnClickListener(6));
        view.findViewById(R.id.presetBtn7).setOnClickListener(new ButtonOnClickListener(7));
        view.findViewById(R.id.presetBtn8).setOnClickListener(new ButtonOnClickListener(8));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (wifiManager == null) {
            wifiManager = WifiManager.getSharedInstance(getContext());
        }

        wifiManager.start();

        String wifiSsid = "fluxFi";
        String wifiPass = "activate";

        wifiConnectDisposable =  wifiManager.connectToWpaSsid(wifiSsid, wifiPass)
                .filter(connection -> connection.connected && connection.ssid.equals(wifiSsid))
                .firstElement()
                .subscribe(wiFiConnection -> {
                            Timber.d("%s %s", wiFiConnection.connected ? "connected to " : "disconnected from ",
                                    wiFiConnection.ssid);


                            if (wiFiConnection.connected && wiFiConnection.ssid.equals(wifiSsid)) {
                                if (oscMdnsManager == null) {
                                    oscMdnsManager = new OscMdnsManager(getActivity().getApplicationContext(), oscClient.Companion.getDefaultLocalPort());
                                }

                                oscMdnsManager.setCallback(this);

                                oscMdnsManager.registerService();
                                oscMdnsManager.discoverPeers();
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

        if (oscMdnsManager != null) {
            oscMdnsManager.release();
            oscMdnsManager = null;
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

    // Mdns Callback

    @Override
    public void onPeerDiscovered(@NotNull String hostName, @NotNull InetAddress hostAddress, int hostPort) {
        Timber.d("Discovered OSC peer at %s %s:%d", hostName, hostAddress, hostPort);

        // TODO : Save Persisted OSC Host
        String primaryOscHostName = gjPrefs.getPrimaryOscHost();
        if (primaryOscHostName == null) {

            if (!dialogHelper.isShowingDialog()) {
                dialogHelper.showDiscoveryDialog(getActivity(), this);
            }

            OscHostDiscoveryDialog.OscHost host = new OscHostDiscoveryDialog.OscHost(hostName, hostAddress, hostPort);
            dialogHelper.onHostDiscovered(host);

            return;
        }

        if (oscClient == null) {

            if (hostName.equals(primaryOscHostName)) {
                connectToOscHost(hostName, hostAddress, hostPort);
            } else {
                Timber.d("Discovered host %s does not match primary host %s", hostName, primaryOscHostName);
            }

        } else {
            Timber.d("Already connected to an OSC client");
        }
    }

    // OSC Host selection dialog callback

    @Override
    public Unit invoke(OscHostDiscoveryDialog.OscHost oscHost) {
        Timber.d("Setting primary OSC Host %s", oscHost.getHostname());
        gjPrefs.setPrimaryOscHost(oscHost.getHostname());
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

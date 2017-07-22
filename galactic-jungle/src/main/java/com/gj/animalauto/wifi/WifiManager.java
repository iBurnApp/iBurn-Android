/*
 * WifiManager.java
 *
 * The contents of this file are confidential and proprietary to Pearl Automation Inc.
 *
 * Any use, reproduction, distribution, and/or transfer of this file is strictly
 * prohibited without the express written permission of the current copyright
 * owner.
 *
 * Any licensed derivative work must retain this notice.
 *
 * Copyright (c) 2015, Pearl Automation Inc. All Rights Reserved.
 */

package com.gj.animalauto.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;


/**
 * Facilitates observing the state of the device's WiFi connection and issuing
 * commands to connect to a given WPA protected WiFi AP.
 * <p>
 * Note that each call to {@link #connectToWpaSsid(String, String)}
 * will set the system state to hunt for the given WiFi SSID. When the target SSID changes, previous
 * subscribers of these methods called with a different SSID will receive .onComplete.
 * <p>
 * Created by davidbrodsky on 7/13/15.
 */
public class WifiManager extends BroadcastReceiver {
    public static final int MIN_WIFI_PASSWORD_CHARACTERS = 8;

    private static final int WIFI_CONNECTION_TIMEOUT_S = 15;

    private Context context;

    public static final String TARGET_WIFI_NOT_FOUND = "targetWifiNotFound";
    public static final String WIFI_NOT_ENABLED = "wifiNotEnabled";
    public static final String AUTHENTICATION_ERROR = "authenticationError";

    @StringDef({
            TARGET_WIFI_NOT_FOUND,
            WIFI_NOT_ENABLED,
            AUTHENTICATION_ERROR
    })
    public @interface WifiExceptionReason {
    }

    public static class WifiException extends Exception {
        @WifiExceptionReason
        private final String wifiExceptionReason;

        public WifiException(@WifiExceptionReason String wifiExceptionReason) {
            this.wifiExceptionReason = wifiExceptionReason;
        }

        @WifiExceptionReason
        public String getWifiExceptionReason() {
            return wifiExceptionReason;
        }
    }

    /**
     * There is a lag between NETWORK_STATE_CHANGED_ACTION receiver reporting SSID
     * change and the Android WIFI_SERVICE reflecting this change. Thus {@link #getCurrentConnectedSsid()}
     * cannot rely on WIFI_SERVICE's report if the method is called in immediate response to a
     * NETWORK_STATE_CHANGED_ACTION receiver action.
     * Instead we manually keep track of the last connected SSID as reported by the
     * NETWORK_STATE_CHANGED_ACTION receiver.
     */
    private String currentlyConnectedSsid;

    /**
     * When we get disconnection events, we aren't given an SSID, so remember the last connected SSID,
     * and report that as the disconnected one.
     */
    private String lastReportedConnectedSsid;
    private String lastReportedDisconnectedSsid;

    // State required for connectToWpaSsid
    private String targetSsid;      // The system can only be attempting to connect to one network at a time
    private int targetSsidNetworkId = -1;
    private BehaviorSubject<WiFiConnection> connectionSubject;
    private PublishSubject<List<ScanResult>> scanSubject;

    private HubWifiAnalytics hubWifiAnalytics;

    private boolean isReceiverRegistered;

    /**
     * Function for filter operator that passes {@link WifiManager.WiFiConnection}s
     * if the ssid is equal to the target, or null (could be a disconnection from target).
     * This filter should be used in conjunction with {@link WifiManager.TargetSsidFilter}
     * in a takeWhile operator so that clients are given the .OnComplete event when the system starts
     * hunting for a different SSID.
     */
    private class WiFiConnectionSsidFilter implements Predicate<WiFiConnection> {

        private String targetSsid;

        public WiFiConnectionSsidFilter(@NonNull String targetSsid) {
            this.targetSsid = targetSsid;
        }

        @Override
        public boolean test(WiFiConnection wiFiConnection) throws Exception {
            return wiFiConnection.ssid == null || equalsExcludingQuotations(wiFiConnection.ssid, targetSsid);
        }
    }

    /**
     * Function for takeWhile operator that passes events while the SSID the system is hunting for
     * matches that the client specified on subscription. This prevents subscribers from receiving
     * events from other SSID hunts.
     */
    private class TargetSsidFilter implements Predicate<WiFiConnection> {

        private String targetSsid;

        public TargetSsidFilter(String targetSsid) {
            this.targetSsid = targetSsid;
        }

        @Override
        public boolean test(WiFiConnection wiFiConnection) throws Exception {
            String managerTarget = WifiManager.this.targetSsid;
            boolean ssidMatches = targetSsid.equals(managerTarget);
            if (!ssidMatches) {
                Timber.w("Target ssid is now '%s'. Filter target is '%s'",
                        managerTarget, targetSsid);
            }
            return ssidMatches;        }
    }

    private static WifiManager sharedInstance;

    public static WifiManager getSharedInstance(@NonNull Context context) {
        if (sharedInstance == null) {
            sharedInstance = new WifiManager(context.getApplicationContext());
        }
        return sharedInstance;
    }

    private WifiManager(Context context) {
        this.context = context;
        hubWifiAnalytics = new HubWifiAnalytics();
    }

    public void start() {
        Timber.d("start(). Registering BroadcastReceiver");
        registerWifiBroadcastReceiver();
        setupSubjects();

        lastReportedConnectedSsid = null;
        lastReportedDisconnectedSsid = null;

        // Initialize currentlyConnectedSsid. Value will be updated via BroadcastReceiver
        NetworkInfo networkInfo = getWifiNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            final android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                currentlyConnectedSsid = connectionInfo.getSSID();
                Timber.d("Setting currently connected network %s", currentlyConnectedSsid);
                updateConnectionSubject(
                        currentlyConnectedSsid,
                        wifiManager.getConnectionInfo().getIpAddress(),
                        true);
            }
        }
    }

    /**
     * Request a WiFi scan and observe the results.
     * NOTE: This will interrupt network traffic. Do not perform during performance-critical
     * networking, like video streaming.
     */
    public Observable<List<ScanResult>> requestScan() {

        setupSubjects();

        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean startedScan = wifiManager.startScan();
        Timber.d("WIFI-SCAN started scan with success %b", startedScan);
        return scanSubject.hide().firstElement().toObservable();
    }

    /**
     * Observe the result of WiFi scans. This will not initiate a scan.
     */
    public Observable<List<ScanResult>> observeScanResults() {
        return scanSubject.hide();
    }

    /**
     * @return {@link NetworkInfo} for the currently connected WiFi network, or null if none are
     * reported by Android
     */
    private NetworkInfo getWifiNetworkInfo() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connManager.getAllNetworks();
            for (Network network : networks) {
                NetworkInfo networkInfo = connManager.getNetworkInfo(network);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    return networkInfo;
                }
            }
        } else {
            return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }

        return null;
    }

    /**
     * @param targetSsid a regular string to match SSID against. Hex strings not supported.
     * @return whether the device is currently connected to the given SSID.
     */
    public boolean isConnectedToSsid(@NonNull String targetSsid) {
        String connectedSsid = getCurrentConnectedSsid();
        if (connectedSsid == null) {
            Timber.d("Connected ssid is null");
            return false; // ssid cannot be null, so the comparison below would have returned false
        }

        Timber.d("isConnectedToSsid(%s). Connected to: %s", targetSsid, connectedSsid);
        // UTF-8 SSIDs are presented surrounded in double quotes
        return equalsExcludingQuotations(connectedSsid, targetSsid);
    }

    private static boolean equalsExcludingQuotations(String firstString, String secondString) {
        return Objects.equals(TextUtils.isEmpty(firstString) ? null : trimQuotes(firstString),
                TextUtils.isEmpty(secondString) ? null : trimQuotes(secondString));
    }

    /**
     * @return target with leading and trailing quotations removed, or the original string
     * if leading and trailing quotations are not present. Currently does not mutate
     * Strings with lead, trail <em>and</em> content quotes
     */
    private static String trimQuotes(@NonNull String target) {
        if (TextUtils.isEmpty(target)) {
            return target;
        }

        if (target.charAt(0) == '"' && target.charAt(target.length() - 1) == '"') {
            String trimmed = target.replaceAll("\"", "");
            return trimmed.length() == target.length() - 2 ? trimmed : target;
        }
        return target;
    }

    /**
     * Disconnect and forget the given ssid. This method may only be called with an SSID
     * registered by our application.
     */
    public void disableSsid(@NonNull final String ssid) {

        boolean removed = removeNetwork(ssid);
        if (!removed) return;

        if (equalsExcludingQuotations(targetSsid, ssid)) {
            // If we're disabling the targetSsid, clear target
            targetSsid = null;
            targetSsidNetworkId = -1;
        }
    }

    public Observable<WiFiConnection> connectToWpaSsid(@NonNull String ssid,
                                                       @NonNull String password) {

        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Timber.d("Wifi is not enabled");
            return Observable.error(new WifiException(WIFI_NOT_ENABLED));
        }

        if (!equalsExcludingQuotations(ssid, targetSsid)) {
            hubWifiAnalytics.beginWifiTimeline();
        }

        final WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";
        targetSsid = ssid;

        wifiConfig.priority = 20;

        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.preSharedKey = "\"" + password + "\"";

        return connectToWifiConfiguration(wifiConfig)
                .filter(new WiFiConnectionSsidFilter(ssid))
                .takeWhile(new TargetSsidFilter(ssid));
//                .timeout(() -> Observable.timer(WIFI_CONNECTION_TIMEOUT_S, TimeUnit.SECONDS)
//                                .doOnNext((ignored) -> /* First emission timeout */ Timber.w("Connection timed out")),  // Update usage in LogAnalyzer before changing this log
//                        conn -> {
//                            // Subsequent item timeout
//                            // If this is a disconnection event, timeout applies to next emission
//                            // If this is a connection event, no timeout applies to next emission
//                            if (!conn.connected) {
//                                return Observable.timer(WIFI_CONNECTION_TIMEOUT_S, TimeUnit.SECONDS)
//                                        .doOnNext((ignored) -> Timber.w("Connection timed out"));
//                            } else {
//                                return Observable.never();
//                            }
//                        },
//                        Observable.error(new WifiException(TARGET_WIFI_NOT_FOUND)));  // Timeout Observable
    }

    private Observable<WiFiConnection> connectToWifiConfiguration(@NonNull WifiConfiguration wifiConfiguration) {
        // Strip leading and trailing quotes
        targetSsid = wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1);
        targetSsidNetworkId = -1;

        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (isConnectedToSsid(targetSsid)) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            updateConnectionSubject(targetSsid,
                    wifiInfo.getIpAddress(),
                    true);
        } else {

            // wifiManager.getConfiguredNetworks() may return null if an error occurs
            // See if an existing network exists
            List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
            if (wifiConfigs != null) {
                for (WifiConfiguration network : wifiConfigs) {
                    if (equalsExcludingQuotations(network.SSID, targetSsid)) {
                        // Now that we remove network, finding an existing configuration indicates
                        // we may have problems removing the network, because it was added by
                        // another package (A previous version of the app, SystemUI etc.)
                        Timber.w("Found matching existing WiFiConfiguration");  // Update usage in LogAnalyzer before changing this log
                        targetSsidNetworkId = network.networkId;
                        break;
                    }
                }
            }

            if (targetSsidNetworkId == -1) {
                // Wifi passwords have to be at least 8 characters
                if (trimQuotes(wifiConfiguration.preSharedKey).length() < MIN_WIFI_PASSWORD_CHARACTERS) {
                    hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiErrorAuth);
                    return Observable.error(new WifiException(AUTHENTICATION_ERROR));
                }
                hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiAddNetwork);

                targetSsidNetworkId = wifiManager.addNetwork(wifiConfiguration);
            }

            if (targetSsidNetworkId == -1) {
                // We were unable to add/update or find an existing configuration for the network
                Timber.w("Unable to add network %s", wifiConfiguration.SSID);
                hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiErrorNotFound);
                return Observable.error(new WifiException(TARGET_WIFI_NOT_FOUND));
            } else {
                Timber.d("Got id %d for %s", targetSsidNetworkId, wifiConfiguration.SSID);
                hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiEnableNetwork);
                boolean success = wifiManager.enableNetwork(targetSsidNetworkId, true);
                if (success) wifiManager.saveConfiguration();
                Timber.d("Issued connection to %s with success %b", wifiConfiguration.SSID, success);  // Update usage in LogAnalyzer before changing this log
            }
        }
        return connectionSubject.hide();
    }


    /**
     * Call after no more WiFi event monitoring is needed in response to calls to
     * {@link #connectToWpaSsid(String, String)}.
     * <p>
     * NOTE: Because this class is a shared instance, all resources released here
     * must be re-created if necessary when they are needed. See {@link #setupSubjects()}.
     */
    public void release() {
        unregisterWifiBroadcastReceiver();

        if (connectionSubject != null) {
            connectionSubject.onComplete();
            Timber.d("deleting connection subject");
            connectionSubject = null;
        }

        if (scanSubject != null) {
            scanSubject.onComplete();
            scanSubject = null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        switch (action) {
            case android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION:
                if (!wifiManager.isWifiEnabled()) {
                    connectionSubjectError(new WifiException(WIFI_NOT_ENABLED));
                }
                break;
            case android.net.wifi.WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                SupplicantState newState = intent.getParcelableExtra(android.net.wifi.WifiManager.EXTRA_NEW_STATE);
                if (newState != null) {
                    hubWifiAnalytics.addWifiTimeLine(newState.name());
                }

                boolean connectedSupplicant = intent.getBooleanExtra(android.net.wifi.WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
                if (!connectedSupplicant) {
                    int error = intent.getIntExtra(android.net.wifi.WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (error == android.net.wifi.WifiManager.ERROR_AUTHENTICATING) {
                        Timber.w("Failed to authenticate wifi network");  // Update usage in LogAnalyzer before changing this log
                        // TODO: Do we definitely not want to reset targetSsid?
                        hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiErrorAuth);
                        removeNetwork(targetSsid);
                        connectionSubjectError(new WifiException(AUTHENTICATION_ERROR));
                    }
                }
                break;
            case android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION:
                NetworkInfo info = intent.getParcelableExtra(android.net.wifi.WifiManager.EXTRA_NETWORK_INFO);
                WifiInfo wifiInfo = intent.getParcelableExtra(android.net.wifi.WifiManager.EXTRA_WIFI_INFO);

                hubWifiAnalytics.addWifiTimeLine(info);

                /* In some devices that have LTE, the system will never broadcast a CONNECTED state.
                   This is because the system doesn't want to connect to a network that doesn't have
                   an internet connection. Instead listen for CONNECTING with VERIFYING_POOR_LINK
                   and assume connected.
                */
                boolean connected = info.isConnected() ||
                        (info.getState() == NetworkInfo.State.CONNECTING
                                && NetworkInfo.DetailedState.VERIFYING_POOR_LINK.equals(info.getDetailedState()));

                String ssid = wifiInfo == null ? null : wifiInfo.getSSID();

                Timber.d("%s %s in state %s. Supplicant state %s",
                        connected ? "Connected to" : "Disconnected from",
                        ssid,
                        info.getState() + " : " + info.getDetailedState(),
                        wifiInfo != null && wifiInfo.getSupplicantState() != null ? wifiInfo.getSupplicantState().name() : "n/a");

                if (connected) {
                    onHubConnected(wifiInfo);
                } else {
                    currentlyConnectedSsid = null;
                }

                if (info.getState() == NetworkInfo.State.DISCONNECTED) {
                    // Don't pass intermediate events like "Connecting"
                    updateConnectionSubject(ssid,
                            wifiInfo == null ? 0 : wifiInfo.getIpAddress(),
                            connected);
                }
                break;
            case android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                List<ScanResult> scanResults = wifiManager.getScanResults();
                Timber.d("WIFI-SCAN Got %d scan results", scanResults == null ? 0 : scanResults.size());  // Update usage in LogAnalyzer before changing this log
                logIfContainsTargetSsid(scanResults);
                if (scanResults != null) {
                    scanSubject.onNext(scanResults);
                }
                break;
        }
    }

    private void updateConnectionSubject(String ssid, int ipAddress, boolean connected) {

        // Don't allocate object for duplicate event. We get ~20 duplicates per connection
        final boolean isDuplicateConnection = connected && Objects.equals(ssid, lastReportedConnectedSsid);
        final boolean isDuplicateDisconnection = !connected && Objects.equals(lastReportedConnectedSsid, lastReportedDisconnectedSsid);

        if (isDuplicateConnection || isDuplicateDisconnection) return;

        String reportedSsid = connected ? ssid : lastReportedConnectedSsid;

        if (reportedSsid == null) {
            Timber.d("No ssid available to report %s event. Ignoring", connected ? "connection" : "disconnection");
            return;
        }

        if (connected) {

            lastReportedConnectedSsid = ssid;

            // Report at most one disconnection per connection.
            // Once a new connection is formed, the last disconnected ssid is irrelevant
            // w.r.t preventing duplicate disconnection reports
            lastReportedDisconnectedSsid = null;

        } else {
            // Disconnected
            Timber.d("Sending disconnection event for ssid %s lastSsid %s", ssid, lastReportedConnectedSsid);
            lastReportedDisconnectedSsid = lastReportedConnectedSsid;

            // Once a disconnect occurs, the last connected ssid is irrelevant
            // w.r.t preventing duplicate connection reports
            lastReportedConnectedSsid = null;
        }


        connectionSubject.onNext(new WiFiConnection(
                reportedSsid,
                ipAddress,
                connected));
    }

    private void setupSubjects() {
        if (connectionSubject == null || connectionSubject.hasComplete() || connectionSubject.hasThrowable()) {
            connectionSubject = BehaviorSubject.create();
            registerWifiBroadcastReceiver();
        }

        if (scanSubject == null || scanSubject.hasComplete() || scanSubject.hasThrowable()) {
            scanSubject = PublishSubject.create();
        }
    }

    private void unregisterWifiBroadcastReceiver() {
        Timber.d("Unregister receiver");
        if (isReceiverRegistered) {
            isReceiverRegistered = false;
            context.unregisterReceiver(this);
        }
    }

    private void registerWifiBroadcastReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(android.net.wifi.WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            intentFilter.addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);
            intentFilter.addAction(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            Timber.d("Registered broadcast receiver");
            context.registerReceiver(this, intentFilter);
            isReceiverRegistered = true;
        }
    }

    /**
     * @return The currently connected SSID or null if not connected
     */
    @Nullable
    private String getCurrentConnectedSsid() {
        return currentlyConnectedSsid;
    }

    public static class WiFiConnection {

        public final String ssid;
        public final int ipAddress;
        public final boolean connected;

        /**
         * Represents a WiFiConnection, or lack thereof
         *
         * @param ssid      Network SSID. May be null if connected is false
         * @param ipAddress ipAddress as an integer. Unused if connected is false
         * @param connected whether this represents a connection (true) or disconnection (false)
         */
        public WiFiConnection(String ssid, int ipAddress, boolean connected) {
            this.ssid = ssid;
            this.ipAddress = ipAddress;
            this.connected = connected;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WiFiConnection that = (WiFiConnection) o;

            if (ipAddress != that.ipAddress) return false;
            if (connected != that.connected) return false;
            return ssid != null ? ssid.equals(that.ssid) : that.ssid == null;

        }

        @Override
        public int hashCode() {
            int result = ssid != null ? ssid.hashCode() : 0;
            result = 31 * result + ipAddress;
            result = 31 * result + (connected ? 1 : 0);
            return result;
        }
    }

    /**
     * @return a String e.g: '192.168.5.2' from an address int
     */
    public static String stringifyIpAddress(int address) {
        return String.format("%d.%d.%d.%d",
                (address & 0xff),
                (address >> 8 & 0xff),
                (address >> 16 & 0xff),
                (address >> 24 & 0xff));
    }

    /**
     * Respond to a Hub WiFi network being reported connected by Android. Note that this may be called
     * multiple times per connection event.
     *
     * @param wifiInfo
     */
    private void onHubConnected(WifiInfo wifiInfo) {
        String ssid = wifiInfo == null ? null : wifiInfo.getSSID();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        currentlyConnectedSsid = ssid;
        if (equalsExcludingQuotations(ssid, targetSsid) && !equalsExcludingQuotations(ssid, lastReportedConnectedSsid)) {
            hubWifiAnalytics.addWifiTimeLine(HubWifiAnalytics.WifiConnected);
            hubWifiAnalytics.endWifiTimeLine();
        }

        updateConnectionSubject(currentlyConnectedSsid,
                ipAddress,
                true);
    }


    @Nullable
    private WifiConfiguration findConfigMatchingSsid(@NonNull String ssid) {
        final android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
        if (wifiConfigs != null) {
            for (WifiConfiguration network : wifiConfigs) {
                if (equalsExcludingQuotations(network.SSID, ssid)) {
                    Timber.d("Found existing WiFiConfiguration for %s", ssid);
                    return network;
                }
            }
        }
        return null;
    }

    private boolean removeNetwork(@NonNull String ssid) {
        final WifiConfiguration config = findConfigMatchingSsid(ssid);
        if (config == null) {
            Timber.w("Cannot remove network with ssid %s. No configuration found", ssid);
            return false;
        }

        final android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean disabled = wifiManager.disableNetwork(config.networkId);
        boolean removed = wifiManager.removeNetwork(config.networkId);
        boolean saved = wifiManager.saveConfiguration();
        Timber.d("Removed known WiFiConfiguration for ssid %s. Disabled: %b Removed: %b Persisted: %b",
                ssid, disabled, removed, saved);

        return removed || disabled;
    }

    private void connectionSubjectError(Throwable throwable) {
        connectionSubject.onError(throwable);
        // After onError an Observable will not emit any other events
        // so we must recreate it
        setupSubjects();
    }

    public Observable<WiFiConnection> observeWifiConnections() {
        return connectionSubject.hide();
    }

    private static class HubWifiAnalytics {
        private static final String WifiAddNetwork = "Wi-Fi.AddNetwork";
        private static final String WifiEnableNetwork = "Wi-Fi.EnableNetwork";
        private static final String WifiConnected = "Wi-Fi.Connected";
        private static final String WifiErrorNotFound = "Wi-Fi.NotFound";
        private static final String WifiErrorAuth = "Wi-Fi.BadAuth";


        private ArrayList<Event> wifiConnectionTimeLine = new ArrayList<>();
        private Date wifiTimeLineStart;

        public void beginWifiTimeline() {
            wifiConnectionTimeLine.clear();
            wifiTimeLineStart = new Date();
        }

        public void addWifiTimeLine(String wifiEvent) {
            wifiConnectionTimeLine.add(new Event(wifiEvent, null));
        }

        private void addWifiTimeLine(NetworkInfo networkInfo) {
            wifiConnectionTimeLine.add(new Event(networkInfo.getState().name(), networkInfo.getDetailedState().name()));
        }

        private void endWifiTimeLine() {
            Date endTime = new Date();

            if (wifiTimeLineStart == null) {
                Timber.d("Cannot log timeline because it wasn't started");
                return;
            }

            if (wifiConnectionTimeLine.size() == 0) {
                Timber.d("Cannot log timeline because there are no events");
                return;
            }

            HashMap<String, Double> escargotWifiConnInfo = new HashMap<>();

            long lastEventTime = wifiTimeLineStart.getTime();

            for (int eventIdx = 0; eventIdx < wifiConnectionTimeLine.size(); eventIdx++) {
                Event event = wifiConnectionTimeLine.get(eventIdx);
                long sinceLast = (event.getTimeMs() - lastEventTime);

                lastEventTime = event.getTimeMs();

                escargotWifiConnInfo.put(eventIdx + "_" + event.getEventName(), (double) sinceLast / 1000);
            }

            escargotWifiConnInfo.put("total.duration", (endTime.getTime() - (double) wifiTimeLineStart.getTime()) / 1000);

        }


        public static class Event {
            final String eventType;
            final String eventDescription;
            final Date date;

            public Event(@NonNull String eventType, String eventDescription) {
                this.eventType = eventType;
                this.eventDescription = eventDescription;
                this.date = new Date();
            }

            public long getTimeMs() {
                return date.getTime();
            }

            public String getEventName() {
                String eventName = eventType;
                if (eventDescription != null) {
                    eventName += "." + eventDescription;
                }
                return eventName;
            }
        }
    }

    /**
     * Generate a random IP address String suitable for use on the Hub's WiFi network.
     * We know that the Hub's DHCP service will issues addresses from 192.168.5.10 - 192.168.5.255
     */
    private static String generateRandomIpAddress() {
        int min = 10;
        int max = 255;
        int addressSuffix = min + (int) (Math.random() * (max - min));

        return String.format(Locale.US, "192.168.5.%d", addressSuffix);
    }

    private void logIfContainsTargetSsid(List<ScanResult> scanResults) {
        if (targetSsid == null) {
            Timber.d("WIFI-SCAN Target ssid is null");
            return;
        }

        boolean found = false;
        for (ScanResult scanResult : scanResults) {
            if (equalsExcludingQuotations(targetSsid, scanResult.SSID)) {
                found = true;
                break;
            }
        }

        if (found) {
            Timber.d("WIFI-SCAN Found target ssid %s", targetSsid);  // Update usage in LogAnalyzer before changing this log
        } else {
            Timber.d("WIFI-SCAN Did not find target ssid %s", targetSsid);
        }

    }
}
package tprz.signaltracker.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The WiFiProfiler listens for changes in the available WiFi networks.
 * It is able to update a StationCard to provide information about the
 * current Station we are at.
 */
public class WifiProfiler {
    private WifiManager wifiManager;
    private Set<String> upToDateScan;
    private Station currentStation = null;
    private StationCard card;
    private TubeGraph tubeGraph;
    private boolean isEnabled;
    private Handler handler;
    private long SCAN_INTERVAL = 7 * 1000;

    BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                if(isEnabled) {
                    List<ScanResult> results = wifiManager.getScanResults();
                    Set<String> macs = new HashSet<>();
                    if (results != null) {
                        for (ScanResult result : results) {
                            if (result.SSID.equalsIgnoreCase("Virgin Media WiFi")) {
                                macs.add(result.BSSID);
                            }
                        }

                        upToDateScan = macs;
                        Log.i("TubeGraph", "Scan results arrived");
                        Station newCurrentStation = getCurrentStation();
                        Log.i("TubeGraph", "new curr station: " + newCurrentStation);
                        if (newCurrentStation != null && !newCurrentStation.equals(currentStation)) {
                            currentStation = newCurrentStation;
                            tubeGraph.addNewStationMapping(newCurrentStation, macs);
                        }

                        LocationFingerprint locationFingerprint =
                                new LocationFingerprint(macs, newCurrentStation == null, tubeGraph, context);
                        updateCard(newCurrentStation, locationFingerprint);
                    }
                }
            }
        }
    };
    private WifiScannerRunnable periodicScanner;
    private static final String TAG =  "WifiProfiler";

    /**
     *
     * @param context Context under which we want to retrieve the WifiManager
     * @param card The StationCard that we want to update on change of station.
     */
    public WifiProfiler(Context context, StationCard card, TubeGraph tubeGraph) {
        this.card = card;
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        upToDateScan = new HashSet<>();
        this.tubeGraph = tubeGraph;
        this.handler = new Handler();

        setupPeriodicWifiScan();

        context.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void setupPeriodicWifiScan() {
        periodicScanner = new WifiScannerRunnable();
        handler.postDelayed(periodicScanner, SCAN_INTERVAL);
    }

    private class WifiScannerRunnable implements Runnable {

        @Override
        public void run() {
            if(isEnabled) {
                startWifiScan();
                Log.i(TAG, "Periodic Wifi Scan Started.");
                handler.postDelayed(this, SCAN_INTERVAL);
            }
        }
    }

    /**
     * Start a WifiScan
     */
    public void startWifiScan() {
        wifiManager.startScan();
    }

    /**
     * Update the provided card informing it of a change in the current
     * station.
     * @param newCurrentStation The current station we are at.
     */
    private void updateCard(Station newCurrentStation, LocationFingerprint locFingerprint) {
        card.updateCard(newCurrentStation, locFingerprint);
    }

    /**
     * Return the current station. The current station is obtained by matching
     * MAC addresses with the Tube Graph which is able to map MAC addresses to
     * stations.
     * @return The station we are currently at.
     */
    private Station getCurrentStation() {
        Map<String, Station> macMap = tubeGraph.getMacsToLocation();

        for(String mac : upToDateScan) {
            if (macMap.containsKey(mac)) {
                return macMap.get(mac);
            }
        }

        return null;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;

        if(isEnabled) {
            periodicScanner.run();
        }
    }

    /**
     * The direction of travel.
     * For lines like the Circle line the actual East/West direction
     * changes even if the destination of the tube. For our purposes if the
     * Circle line is going Counter-Clockwise then it is going East and
     * Clockwise is West.
     */
    public enum Direction{
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

}

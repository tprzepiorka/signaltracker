package tprz.signaltracker.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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

    BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                List<ScanResult> results = wifiManager.getScanResults();
                Set<String> ssids = new HashSet<>();
                if (results != null) {
                    for (ScanResult result : results) {
                        ssids.add(result.BSSID);
                    }

                    upToDateScan = ssids;
                    Log.i("TubeGraph", "Scan results arrived");
                    Station newCurrentStation = getCurrentStation();
                    Log.i("TubeGraph", "new curr station: " + newCurrentStation);
                    if(newCurrentStation != null && !newCurrentStation.equals(currentStation) ) {
                        currentStation = newCurrentStation;
                        tubeGraph.addNewStationMapping(newCurrentStation, ssids);
                    }

                    LocationFingerprint locationFingerprint = new LocationFingerprint(ssids, newCurrentStation == null, tubeGraph);
                    updateCard(newCurrentStation, locationFingerprint);
                }
            }
        }
    };

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


        context.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
        Map<String, Station> ssidMap = tubeGraph.getSsidsToLocation();

        for(String ssid : upToDateScan) {
            if (ssidMap.containsKey(ssid)) {
                return ssidMap.get(ssid);
            }
        }

        return null;
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

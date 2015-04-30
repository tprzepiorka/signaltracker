package tprz.signaltracker.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.gmariotti.cardslib.library.internal.Card;
import tprz.signaltracker.CellSignalCard;
import tprz.signaltracker.StationLocationCard;

/**
 * Created by tomprz on 30/04/2015.
 */
public class WifiProfiler {
    private WifiManager wifiManager;
    private Set<String> upToDateScan;
    private Station currentStation = null;
    private CellSignalCard card;
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
                        updateCard(newCurrentStation);
                        tubeGraph.addNewStationMapping(newCurrentStation, ssids);
                    }
                }
            }
        }
    };

    public WifiProfiler(Context context, CellSignalCard card) {
        this.card = card;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        upToDateScan = new HashSet<>();
        tubeGraph = new TubeGraph();


        context.registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void updateCard(Station newCurrentStation) {
        card.updateCard(newCurrentStation);
    }

    private Station getCurrentStation() {
        Map<String, Station> ssidMap = tubeGraph.getSsidsToLocation();

        for(String ssid : upToDateScan) {
            if (ssidMap.containsKey(ssid)) {
                return ssidMap.get(ssid);
            }
        }

        return null;
    }

    public enum Direction{
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    public Set<String> getWifiAccessPointsMac() {
        wifiManager.startScan();
        return upToDateScan;
    }
}

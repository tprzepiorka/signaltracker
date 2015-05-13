package tprz.signaltracker.location;

import android.content.Context;

import java.util.Set;

import tprz.signaltracker.DataReporter;


/**
 * Provides an encapsulation around the results of a WiFi search allowing a new previously
 * unknown set of MAC addresses to be mapped to a station.
 */
public class LocationFingerprint {
    private final DataReporter dataReporter;
    private Set<String> macAddresses;
    private boolean isUnknown;
    private TubeGraph tubeGraph;

    /**
     * Create a LocationIdentity
     * @param macAddresses Mac Addresses that belong to this location fingerprint.
     * @param isUnknown Is there already a known station associated with this fingerprint.
     * @param tubeGraph The graph of stations and MAC address mappings.
     */
    public LocationFingerprint(Set<String> macAddresses, boolean isUnknown, TubeGraph tubeGraph, Context context) {
        this.macAddresses = macAddresses;
        this.isUnknown = isUnknown;
        this.tubeGraph = tubeGraph;
        this.dataReporter = DataReporter.getInstance(context);
    }

    /**
     * Identify this set of MAC addresses as belonging to the given station.
     * This identification is persisted to the TubeGraph file.
     * @param station The station that this location fingerprint belongs to.
     */
    public boolean mapNewStation(Station station) {
        String[] macAddressesArray = new String[macAddresses.size()];
        macAddresses.toArray(macAddressesArray);
        dataReporter.addStationMacs(station.getName(), macAddressesArray);
        return isUnknown && tubeGraph.addNewStationMapping(station, macAddresses);
    }
}

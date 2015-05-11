package tprz.signaltracker.location;

import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TubeGraph loads the graph data representing the tube map into objects
 */
public class TubeGraph {
    private static final String TAG = "TubeGraph";
    private Map<String, Station> macsToLocation;
    private Map<String, Station> stationMap;
    private JSONArray stations;
    private String tubeGraphPath;
    private JSONObject tubeGraph;

    public TubeGraph() {
        macsToLocation = new HashMap<>();
        stationMap = new HashMap<>();
        tubeGraphPath = Environment.getExternalStorageDirectory() + "/tubeGraphTest.json";
        loadFromFile();
    }

    /**
     * Load the tubegraph from a file into the data structures of the class.
     * Can be called at any point to refresh the state of the instance with new contents
     * from the json file.
     */
    private void loadFromFile() {
        try {
            String content = getStringFromFile(tubeGraphPath);
            tubeGraph = new JSONObject(content);

            buildStations(tubeGraph);
            buildEdges(tubeGraph);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Build the edges of the graph, representing linking stations.
     * @param tubeGraph The JsonObject with Edge data. It should be the plain object
     *                  in the form read in from the file on storage.
     */
    private void buildEdges(JSONObject tubeGraph) {
        try {
            if(tubeGraph.has("elements")) {
                stations = tubeGraph.getJSONArray("elements");
            } else {
                stations = tubeGraph.getJSONArray("stations");
            }

            for(int i = 0; i < stations.length(); i++) {
                JSONObject stationJson = stations.getJSONObject(i);
                String stationName = stationJson.getString("name");
                if(stationMap.containsKey(stationName)) {
                    Station station = stationMap.get(stationName);
                    List<Edge> east = buildEachEdge(stationJson.getJSONArray("east"), station);
                    List<Edge> west = buildEachEdge(stationJson.getJSONArray("west"), station);
                    station.east = east;
                    station.west = west;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Build a single edge from a starting station
     * @param edges The edges for the given station
     * @param start The starting station for this directed edge
     * @return A list of edge objects for the starting station.
     * @throws JSONException
     */
    private List<Edge> buildEachEdge(JSONArray edges, Station start) throws JSONException {
        List<Edge> edgesSet = new ArrayList<>();

        for(int i = 0; i < edges.length(); i++) {
            JSONObject edge = edges.getJSONObject(i);
            String endName = edge.getString("name");
            int duration = edge.getInt("duration");
            if(stationMap.containsKey(endName)) {
                Edge edgeObject = new Edge(start, stationMap.get(endName), duration);
                edgesSet.add(edgeObject);
            }
        }

        return edgesSet;
    }

    /**
     * Build the station objects for this tubegraph
     * @param tubeGraph The raw tubegraph json object.
     */
    private void buildStations(JSONObject tubeGraph) {
        try {
            if(tubeGraph.has("elements")) {
                stations = tubeGraph.getJSONArray("elements");
            } else {
                stations = tubeGraph.getJSONArray("stations");
            }
            for(int i = 0; i < stations.length(); i++) {
                JSONObject stationJson = stations.getJSONObject(i);
                Map<String, Double> signalStrengths =  getSignalStrengths(stationJson.getJSONArray("signalStrengths"));
                Set<String> ssids = getMacs(stationJson.getJSONArray("macs"));

                Station station = new Station(stationJson.getString("name"), signalStrengths, ssids);
                stationMap.put(station.name, station);

                for(String ssid : ssids) {
                    macsToLocation.put(ssid, station);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get the mapping of Macs to Locations allowing quick finding of our current location.
     * @return Map of MAC addresses to locations.
     */
    public Map<String, Station> getMacsToLocation() {
        return macsToLocation;
    }

    /**
     * Build the Set of MAC addresses from a JSON array of Strings where each element is a SSID
     * @param macs JSONArray of MAC addresses stored as String primatives
     * @return A set of Stings where each String is a MAC address
     * @throws JSONException
     */
    private Set<String> getMacs(JSONArray macs) throws JSONException {
        Set<String> macSet = new HashSet<>();

        for(int i = 0; i < macs.length(); i++) {
            String mac = macs.getString(i);
            macSet.add(mac);
        }

        return macSet;
    }

    /**
     * Build the signal strength mapping object from JSON. Will use the average signal strength
     * if it exists, falling back on the single given signal strength.
     * @param signalStrengths JSONArray containing the signal strength readings and operators for
     *                        a given station.
     * @return Mapping of operator to signal strength for a given location.
     * @throws JSONException
     */
    private Map<String, Double> getSignalStrengths(JSONArray signalStrengths) throws JSONException {
        Map<String, Double> signalStrengthsMap = new HashMap<>();

        for(int i = 0; i < signalStrengths.length(); i++) {
            JSONObject entry = signalStrengths.getJSONObject(i);
            if(entry.has("total") && entry.has("count")) {
                int total = entry.getInt("total");
                int count = entry.getInt("count");
                double averageSignalStrength = total/count;
                signalStrengthsMap.put(entry.getString("operator"), averageSignalStrength);
            } else if(entry.has("strength")) {
                signalStrengthsMap.put(entry.getString("operator"), entry.getDouble("strength"));
            }
        }

        return signalStrengthsMap;
    }

    /**
     * Used to read the contents of the json file in.
     * @param is Input stream
     * @return Sring of the input stream
     * @throws Exception
     */
    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Read a file in as a String
     * @param filePath Path to file to be read.
     * @return String contents of a file.
     * @throws Exception
     */
    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    /**
     * Add a new mapping of a station to a set of MAC addresses. The station may already have a
     * mapping and this will simply append on the newly identified MAC addresses to this mapping.
     * Mapping is persisted and kept and may be uploaded through the DataReporter.
     * @param newCurrentStation The station being mapped
     * @param macs The MAC addresses associated with this station.
     * @return True if added succesfully, false otherwise
     */
    public boolean addNewStationMapping(Station newCurrentStation, Set<String> macs) {
        try{
            for (int i = 0; i < stations.length(); i++) {
                JSONObject stationObject = stations.getJSONObject(i);
                if(newCurrentStation.name.equals(stationObject.getString("name"))) {
                    Log.i(TAG, "Matched station name. Attempting to add other matching macs");

                    JSONArray macsJson = stationObject.getJSONArray("macs");
                    Set<String> oldMacs = new HashSet<>(macsJson.length());
                    Log.i(TAG, "Adding " + macs.size() + " possible macs");

                    for(int j = 0; j < macsJson.length(); j++) {
                        oldMacs.add(macsJson.getString(j));
                    }

                    // Update with new macs
                    macs.removeAll(oldMacs);
                    if(!macs.isEmpty()) {
                        Log.i(TAG, "New macs found. ");
                        for(String mac : macs) {
                            macsJson.put(mac);
                        }

                        writeTubeGraph(tubeGraph.toString(4));
                        loadFromFile();
                        return true;
                    }

                }
            }
        } catch (JSONException | IOException e) {
            Log.i(TAG, "Error: " + e);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Return a Station object that has the same name as the provided string.
     * @param stationName The name of the station, with exact match
     * @return A station object if it exists, false otherwise.
     */
    public Station getStationByName(String stationName) {
        return stationMap.get(stationName);
    }

    /**
     * Overwrite the existing tube graph
     * @param graphStringToWrite New tubegraph to write to storage.
     * @throws IOException
     * @throws JSONException
     */
    private void writeTubeGraph(String graphStringToWrite) throws IOException, JSONException {
        FileWriter file = new FileWriter(tubeGraphPath);
        try {
            file.write(graphStringToWrite);
            Log.i(TAG, "Successfully wrote update to file.");

            System.out.println("Successfully Copied JSON Object to File...");

        } catch (IOException e) {
            Log.i(TAG, "Error: " + e);
            e.printStackTrace();
        } finally {
            file.flush();
            file.close();
        }
    }

    /**
     * Update the tubegraph with a new one.
     * @param newGraph New graph to take the place of the existing one.
     */
    public void update(JsonObject newGraph) {
        try {
            writeTubeGraph(newGraph.toString());
            loadFromFile();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error: " + e);
            e.printStackTrace();
        }
    }
}

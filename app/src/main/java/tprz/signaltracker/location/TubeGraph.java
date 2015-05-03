package tprz.signaltracker.location;

import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonElement;

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
    private Map<String, Station> ssidsToLocation;
    private Map<String, Station> stationMap;
    private JSONArray stations;
    private String tubeGraphPath;
    private JSONObject tubeGraph;

    public TubeGraph() {
        ssidsToLocation = new HashMap<>();
        stationMap = new HashMap<>();
        tubeGraphPath = Environment.getExternalStorageDirectory() + "/tubeGraphTest.json";
        loadFromFile();
    }

    public Map<String, Station> getSsidsToLocation() {
        return ssidsToLocation;
    }

    public void loadFromFile() {
        try {
            String content = getStringFromFile(tubeGraphPath);
            tubeGraph = new JSONObject(content);

            buildStations(tubeGraph);
            buildEdges(tubeGraph);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildEdges(JSONObject tubeGraph) {
        try {
            stations = tubeGraph.getJSONArray("stations");
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

    private void buildStations(JSONObject tubeGraph) {
        try {
            stations = tubeGraph.getJSONArray("stations");
            for(int i = 0; i < stations.length(); i++) {
                JSONObject stationJson = stations.getJSONObject(i);
                Map<String, Double> signalStrengths =  getSignalStrengths(stationJson.getJSONArray("signalStrengths"));
                Set<String> ssids = getSsids(stationJson.getJSONArray("ssids"));

                Station station = new Station(stationJson.getString("name"), signalStrengths, ssids);
                stationMap.put(station.name, station);

                for(String ssid : ssids) {
                    ssidsToLocation.put(ssid, station);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Set<String> getSsids(JSONArray ssids) throws JSONException {
        Set<String> ssidsSet = new HashSet<>();

        for(int i = 0; i < ssids.length(); i++) {
            String ssid = ssids.getString(i);
            ssidsSet.add(ssid);
        }

        return ssidsSet;
    }

    private Map<String, Double> getSignalStrengths(JSONArray signalStrengths) throws JSONException {
        Map<String, Double> signalStrengthsMap = new HashMap<>();

        for(int i = 0; i < signalStrengths.length(); i++) {
            JSONObject entry = signalStrengths.getJSONObject(i);
            signalStrengthsMap.put(entry.getString("operator"), entry.getDouble("strength"));
        }

        return signalStrengthsMap;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public void addNewStationMapping(Station newCurrentStation, Set<String> ssids) {
        try{
            for (int i = 0; i < stations.length(); i++) {
                JSONObject stationObject = stations.getJSONObject(i);
                if(newCurrentStation.name.equals(stationObject.getString("name"))) {
                    Log.i(TAG, "Matched station name. Attempting to add other matching ssids");

                    JSONArray ssidsJson = stationObject.getJSONArray("ssids");
                    Set<String> oldSsids = new HashSet<>(ssidsJson.length());
                    Log.i(TAG, "Adding " + ssids.size() + " possible ssids");

                    for(int j = 0; j < ssidsJson.length(); j++) {
                        oldSsids.add(ssidsJson.getString(j));
                    }

                    // Update with new ssids
                    ssids.removeAll(oldSsids);
                    if(!ssids.isEmpty()) {
                        Log.i(TAG, "New ssids found. ");
                        for(String ssid : ssids) {
                            ssidsJson.put(ssid);
                        }

                        writeTubeGraph();
                    }

                }
            }
        } catch (JSONException | IOException e) {
            Log.i(TAG, "Error: " + e.toString());
            e.printStackTrace();
        }
    }

    public Station getStationByName(String stationName) {
        return stationMap.get(stationName);
    }

    private void writeTubeGraph() throws IOException, JSONException {
        FileWriter file = new FileWriter(tubeGraphPath);
        try {
            file.write(tubeGraph.toString(4));
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
}

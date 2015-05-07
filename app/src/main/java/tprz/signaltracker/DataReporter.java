package tprz.signaltracker;

import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import tprz.signaltracker.location.TubeGraph;

/**
 * Connect and send data to the server and update the tubegraph to contain the most recent results.
 */
public class DataReporter {
    private final SigTrackWebService service;
    private String signalReadingsFilePath = Environment.getExternalStorageDirectory() + "/signals.json";
    private String macMappingFilePath = Environment.getExternalStorageDirectory() + "/macMapping.json";
    private JsonArray signalReadings;
    private JsonArray macMapping;
    private TubeGraph tubeGraph;

    private static DataReporter instance = null;
    private final String TAG = "DataReporter";

    private DataReporter() {
        tubeGraph = new TubeGraph();
        try {
            File signalReadingsFile = new File(signalReadingsFilePath);
            if(signalReadingsFile.exists()){
                String content = TubeGraph.getStringFromFile(signalReadingsFilePath);
                JsonParser parser = new JsonParser();
                signalReadings = (JsonArray)parser.parse(content);
            }
            else{
                // create an new file
                File urlconfig = new File(Environment.getExternalStorageDirectory(), "/signals.json");
                urlconfig.createNewFile();
                signalReadings = new JsonArray();
            }

            File macMappingReadingsFile = new File(macMappingFilePath);
            if(macMappingReadingsFile.exists()){
                String content = TubeGraph.getStringFromFile(macMappingFilePath);
                JsonParser parser = new JsonParser();
                macMapping = (JsonArray)parser.parse(content);
                if(macMapping == null) {
                    macMapping = new JsonArray();
                }
            }
            else{
                // create an new file
                File urlconfig = new File(Environment.getExternalStorageDirectory(), "/macMapping.json");
                urlconfig.createNewFile();
                macMapping = new JsonArray();
            }

        } catch (Exception e) {
            MultiLogger.log(TAG, "Error setting up.");
            e.printStackTrace();
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://sigtrackweb.herokuapp.com/")
                .build();

        service = restAdapter.create(SigTrackWebService.class);

    }

    public void addStationMacs(String stationName, String[] macs) {
        Log.i(TAG, "Adding station mac mapping");

        JsonObject stationObject = null;
        for(JsonElement elem : macMapping) {
            if(elem instanceof JsonObject) {
                JsonObject potentialStation = (JsonObject)elem;
                String name = potentialStation.get("name").getAsString();
                if(name.equals(stationName)) {
                    stationObject = potentialStation;
                    break;
                }
            }
        }

        if(stationObject == null) {
            stationObject = new JsonObject();
            stationObject.addProperty("name", stationName);
            stationObject.add("macs", new JsonArray());
            macMapping.add(stationObject);
        }

        JsonArray macArray = stationObject.getAsJsonArray("macs");
        for(String mac : macs) {
            macArray.add(new JsonPrimitive(mac));
        }

        // Save changes
        try {
            writeJson(macMapping, macMappingFilePath);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    public void addSignalReading(String stationName, String operator, int signalStrength) {
        Log.i(TAG, "Adding signal reading: " + stationName + " " + operator + " " + signalStrength);

        JsonObject stationObject = null;
        for(JsonElement elem : signalReadings) {
            if(elem instanceof JsonObject) {
                JsonObject potentialStation = (JsonObject)elem;
                String name = potentialStation.get("name").getAsString();
                String existingOperator = potentialStation.get("operator").getAsString();
                if(name.equals(stationName) && existingOperator.equals(operator)) {
                    stationObject = potentialStation;
                    break;
                }
            }
        }

        if(stationObject == null) {
            stationObject = new JsonObject();
            stationObject.addProperty("name", stationName);
            stationObject.addProperty("operator", operator);
            stationObject.addProperty("total", 0);
            stationObject.addProperty("count", 0);
            signalReadings.add(stationObject);
        }

        int existingTotal = stationObject.get("total").getAsInt();
        int existingCount = stationObject.get("count").getAsInt();

        stationObject.addProperty("total", existingTotal + signalStrength);
        stationObject.addProperty("count", existingCount + 1);

        // Save changes
        try {
            writeJson(signalReadings, signalReadingsFilePath);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void writeJson(JsonElement json, String filePath) throws IOException, JSONException {
        FileWriter file = new FileWriter(filePath);
        try {
            file.write(json.toString());
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

    public TubeGraph getTubeGraph() {
        return tubeGraph;
    }

    public static DataReporter getInstance() {
        if(instance == null) {
            instance = new DataReporter();
        }

        return instance;
    }


    public void sync() {

        service.addSignals(signalReadings, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject integer, Response response) {
                Log.i(TAG, "Successfully added signals to " + integer + " stations.");
                clearSignals();

                service.addStations(macMapping, new Callback<JsonObject>() {
                    @Override
                    public void success(JsonObject jsonObject, Response response) {
                        Log.i(TAG, "Successfully added station macs");
                        clearStationsMappingFiles();

                        service.getLatestTubeGraph(new Callback<JsonElement>() {
                            @Override
                            public void success(JsonElement result, Response response) {
                                Log.i(TAG, "Successfully got new tubegraph");

                                updateTubeGraph(result.getAsJsonObject().getAsJsonArray("elements"));
                            }

                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.e(TAG, "Failed to update tubegraph: " + retrofitError);
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e(TAG, "Failed to add station mac mapping: " + retrofitError);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to add signals: " + error);
            }
        });


    }

    private void updateTubeGraph(JsonArray newTubeGraph) {
        JsonObject holderObject = new JsonObject();
        holderObject.add("stations", newTubeGraph);
        tubeGraph.update(holderObject);
    }

    /**
     * On successfully sending the signal readings to server clear the persisted data file
     * and the current data object.
     */
    private void clearSignals() {
        try {
            writeJson(new JsonArray(), signalReadingsFilePath);
            signalReadings = new JsonArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "Failed to clear file for signals.");
        }
    }

    private void clearStationsMappingFiles() {
        try {
            writeJson(new JsonArray(), macMappingFilePath);
            macMapping = new JsonArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "Failed to clear file for macs.");
        }
    }
}

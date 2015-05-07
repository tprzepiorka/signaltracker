package tprz.signaltracker;

import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
    private JsonObject macMapping;

    private static DataReporter instance = null;
    private final String TAG = "DataReporter";

    private DataReporter() {
        try {
            File signalReadingsFile = new File(signalReadingsFilePath);
            if(signalReadingsFile.exists()){
                String content = TubeGraph.getStringFromFile(signalReadingsFilePath);
                JsonParser parser = new JsonParser();
                signalReadings = (JsonArray)parser.parse(content);
            }
            else{
                // create an new file
                File urlconfig = new File(Environment.getExternalStorageDirectory(), "signals.json");
                urlconfig.createNewFile();
                signalReadings = new JsonArray();
            }


            //content = TubeGraph.getStringFromFile(macMappingFilePath);
          //  macMapping = (JsonObject)parser.parse(content);


        } catch (Exception e) {
            MultiLogger.log(TAG, "Error setting up.");
            e.printStackTrace();
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://sigtrackweb.herokuapp.com/")
                .build();

        service = restAdapter.create(SigTrackWebService.class);

    }

    public void addSignalReading(String stationName, String operator, int signalStrength) {
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

    public static DataReporter getInstance() {
        if(instance == null) {
            instance = new DataReporter();
        }

        return instance;
    }


    public void sync() {
        addSignalReading("South Kensington", "O2", 12);

        service.addSignals(signalReadings, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject integer, Response response) {
                Log.i(TAG, "Successfully added signals to " + integer + " stations.");
                
            }

            @Override
            public void failure(RetrofitError error) {
                Log.i(TAG, "Failed to add signals: " + error);
            }
        });
    }
}

package tprz.signaltracker;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.splunk.mint.Mint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import tprz.signaltracker.location.TubeGraph;
import tprz.signaltracker.reporter.SigTrackWebService;
import tprz.signaltracker.reporter.logs.DataLog;
import tprz.signaltracker.reporter.logs.ObjectFilePair;

/**
 * Connect and send data to the server and update the tubegraph to contain the most recent results.
 */
public class DataReporter {
    private final SigTrackWebService service;
    private final Context context;
    private String signalReadingsFilePath = Environment.getExternalStorageDirectory() + "/signalTracker" + "/signals.json";
    private String macMappingFilePath = Environment.getExternalStorageDirectory() + "/signalTracker" + "/macMapping.json";
    private JsonArray signalReadings;
    private JsonArray macMapping;
    private TubeGraph tubeGraph;
    private DataLog signalLog;

    private static DataReporter instance = null;
    private final String TAG = "DataReporter";

    private DataReporter(Context context) {
        tubeGraph = new TubeGraph(context);
        this.context = context;
        String envPath = Environment.getExternalStorageDirectory() + "/signalTracker";
        this.signalLog = new DataLog(envPath, "signalLog");



        try {
            File signalReadingsFile = new File(signalReadingsFilePath);
            if(signalReadingsFile.exists()){
                String content = TubeGraph.getStringFromFile(signalReadingsFilePath);
                JsonParser parser = new JsonParser();
                signalReadings = (JsonArray)parser.parse(content);
            }
            else{
                // create an new file
                File urlconfig = new File(signalReadingsFilePath);
                urlconfig.createNewFile();
                signalReadings = new JsonArray();
            }
        } catch (Exception e) {
            MultiLogger.log(TAG, "Error setting up.");
            e.printStackTrace();
            Mint.logExceptionMessage("level", "Error setting up signalReadings file", e);
        }

        try{
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
                File urlconfig = new File(signalReadingsFilePath);
                urlconfig.createNewFile();
                macMapping = new JsonArray();
            }

        } catch (Exception e) {
            MultiLogger.log(TAG, "Error setting up .");
            e.printStackTrace();
            Mint.logExceptionMessage("level", "Error setting up MacMapping file", e);
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://sigtrackweb.herokuapp.com/")
                .build();

        service = restAdapter.create(SigTrackWebService.class);

    }

    /**
     * Add station MACs and send these to the server.
     * @param stationName Name of station to add to.
     * @param macs MAC addresses for this station.
     */
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
            Mint.logException(e);
        }

    }

    /**
     * Send signal readings for a given operator at a given station to the server.
     * @param stationName Name of station
     * @param operator Operator name
     * @param signalStrength Signal strength in ASU
     */
    public void addSignalReading(String stationName, String operator, String operatorNumber, int signalStrength) {
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

        stationObject.addProperty("operatorNumber", operatorNumber);

        stationObject.addProperty("device", Build.DEVICE);
        stationObject.addProperty("brand", Build.BRAND);
        stationObject.addProperty("fingerprint", Build.FINGERPRINT);
        stationObject.addProperty("hardware", Build.HARDWARE);
        stationObject.addProperty("manufacturer", Build.MANUFACTURER);
        stationObject.addProperty("model", Build.MODEL);
        stationObject.addProperty("radio", Build.getRadioVersion());
        stationObject.addProperty("serial", Build.SERIAL);
        stationObject.addProperty("user", Build.USER);
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        stationObject.addProperty("androidId", android_id);

        int existingTotal = stationObject.get("total").getAsInt();
        int existingCount = stationObject.get("count").getAsInt();

        stationObject.addProperty("total", existingTotal + signalStrength);
        stationObject.addProperty("count", existingCount + 1);


        // Save changes
        try {
            writeJson(signalReadings, signalReadingsFilePath);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Mint.logException(e);
        }

        // Save changes for details
        JsonParser parser = new JsonParser();
        JsonObject detailedStationObject = (JsonObject)parser.parse(stationObject.toString());
        detailedStationObject.remove("total");
        detailedStationObject.remove("count");
        detailedStationObject.addProperty("signalStrength", signalStrength);
        detailedStationObject.addProperty("timestamp", System.currentTimeMillis() / 1000L);

        signalLog.addLogObject(detailedStationObject);
    }

    /**
     * Write json file to a given filepath.
     * @param json json to write.
     * @param filePath path to file to write to.
     * @throws IOException
     * @throws JSONException
     */
    private void writeJson(JsonElement json, String filePath) throws IOException, JSONException {
        FileWriter file = new FileWriter(filePath);
        try {
            file.write(json.toString());
            Log.i(TAG, "Successfully wrote update to file.");

            System.out.println("Successfully Copied JSON Object to File...");

        } catch (IOException e) {
            Log.i(TAG, "Error: " + e);
            e.printStackTrace();
            Mint.logException(e);
        } finally {
            file.flush();
            file.close();
        }
    }

    /**
     * @return The tubeGraph Json representation. This will be updated on sync results from server.
     */
    public TubeGraph getTubeGraph() {
        return tubeGraph;
    }

    /**
     * Get an instance of the singleton DataReporter
     * @param context Context of application to start Wifi scan and make Toasts
     * @return An instance of DataReporter.
     */
    public static DataReporter getInstance(Context context) {
        if(instance == null) {
            instance = new DataReporter(context);
        }

        return instance;
    }

    /**
     * Send new Location MAC mappings and Signal Strengths to the server. If these have been sent
     * successfully then we clear their files on storage and update our TubeGraph representation
     * with one from the server.
     */
    public void sync() {

        final EventLogger eventLogger = EventLogger.getInstance(context);

        eventLogger.logEvent("Sync started");

        service.addSignals(signalReadings, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject integer, Response response) {
                Log.i(TAG, "Successfully added signals to " + integer + " stations.");
                eventLogger.logEvent("Successfully added signals to stations");
                clearSignals();

                service.addStations(macMapping, new Callback<JsonObject>() {
                    @Override
                    public void success(JsonObject jsonObject, Response response) {
                        Log.i(TAG, "Successfully added station macs");
                        eventLogger.logEvent("Successfully added station macs");
                        clearStationsMappingFiles();

                        service.getLatestTubeGraph(new Callback<JsonElement>() {
                            @Override
                            public void success(JsonElement result, Response response) {
                                Log.i(TAG, "Successfully got new tubegraph");
                                eventLogger.logEvent("Succesfully got new tubegraph");

                                updateTubeGraph(result.getAsJsonObject().getAsJsonArray("elements"));
                                Toast.makeText(context, "Sync Complete.", Toast.LENGTH_SHORT).show();
                                startWifiScan();
                            }

                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.e(TAG, "Failed to update tubegraph: " + retrofitError);
                                Toast.makeText(context, "Sync failed (tubegraph).", Toast.LENGTH_SHORT).show();
                                Mint.logException(retrofitError);
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e(TAG, "Failed to add station mac mapping: " + retrofitError);
                        Toast.makeText(context, "Sync failed (macmapping).", Toast.LENGTH_SHORT).show();
                        Mint.logException(retrofitError);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to add signals: " + error);
                Toast.makeText(context, "Sync failed(signals).", Toast.LENGTH_SHORT).show();
                Mint.logException(error);
            }
        });

        ObjectFilePair[] signalLogSets = signalLog.getSignalLogObjects();
        for(int i = 0; i < signalLogSets.length; i++) {
            final int fileNumber = i;
            final ObjectFilePair currLogSet = signalLogSets[i];
            if(currLogSet == null) continue;

            service.addSignalsDetails(currLogSet.getJson(), new Callback<JsonObject>() {
                @Override
                public void success(JsonObject integer, Response response) {
                    Log.i(TAG, "Successfully added signals details to " + integer + " stations.");
                    eventLogger.logEvent("Successfully added signal details to " + integer + " stations.");
                    signalLog.clearFile(currLogSet.getFileName());
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Failed to add signals Details: " + error);
                    Toast.makeText(context, "Sync failed(signalsDetails).", Toast.LENGTH_SHORT).show();
                    Mint.logException(error);
                }
            });
        }

    }

    private void startWifiScan() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
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
            Mint.logException(e);
        }
    }

    /**
     * On successfully sending the station MAC mapping readings to server clear the persisted data file
     * and the current data object.
     */
    private void clearStationsMappingFiles() {
        try {
            writeJson(new JsonArray(), macMappingFilePath);
            macMapping = new JsonArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "Failed to clear file for macs.");
            Mint.logException(e);
        }
    }
}

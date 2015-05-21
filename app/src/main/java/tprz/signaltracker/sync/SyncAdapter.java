package tprz.signaltracker.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.splunk.mint.Mint;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import tprz.signaltracker.DataReporter;
import tprz.signaltracker.EventLogger;
import tprz.signaltracker.location.TubeGraph;
import tprz.signaltracker.reporter.SigTrackWebService;
import tprz.signaltracker.reporter.logs.DataLog;
import tprz.signaltracker.reporter.logs.ObjectFilePair;

/**
 * Sync data withour server.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";
    private final TubeGraph tubeGraph;
    private SigTrackWebService service;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        initService();
        this.tubeGraph = TubeGraph.getInstance(getContext());
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        initService();
        this.tubeGraph = TubeGraph.getInstance(getContext());
    }

    private void initService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://sigtrackweb.herokuapp.com/")
                .build();

        service = restAdapter.create(SigTrackWebService.class);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, final SyncResult syncResult) {
        Log.i(TAG, "Auto Sync started.");
        final EventLogger eventLogger = EventLogger.getInstance(getContext());
        eventLogger.logEvent("Auto Sync started");

        final DataReporter reporter = DataReporter.getInstance(getContext());
        JsonElement signalReadings = reporter.getSignalReadings();
        final JsonElement macMapping = reporter.getMacMapping();
        final DataLog signalLog = reporter.getSignalLog();

        final Context context = getContext();

        service.addSignals(signalReadings, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject integer, Response response) {
                Log.i(TAG, "Successfully added signals to " + integer + " stations.");
                eventLogger.logEvent("Successfully added signals to stations");
                reporter.clearSignals();
                syncResult.stats.numUpdates++;

                service.addStations(macMapping, new Callback<JsonObject>() {
                    @Override
                    public void success(JsonObject jsonObject, Response response) {
                        Log.i(TAG, "Successfully added station macs");
                        eventLogger.logEvent("Successfully added station macs");
                        reporter.clearStationsMappingFiles();
                        syncResult.stats.numUpdates++;

                        service.getLatestTubeGraph(new Callback<JsonElement>() {
                            @Override
                            public void success(JsonElement result, Response response) {
                                Log.i(TAG, "Successfully got new tubegraph");
                                eventLogger.logEvent("Succesfully got new tubegraph");

                                reporter.updateTubeGraph(result.getAsJsonObject().getAsJsonArray("elements"));
                                Toast.makeText(context, "Auto Sync Complete.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.e(TAG, "Failed to update tubegraph: " + retrofitError);
                                Toast.makeText(context, "Sync failed (tubegraph).", Toast.LENGTH_SHORT).show();
                                Mint.logException(retrofitError);
                                syncResult.stats.numIoExceptions++;
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e(TAG, "Failed to add station mac mapping: " + retrofitError);
                        Toast.makeText(context, "Sync failed (macmapping).", Toast.LENGTH_SHORT).show();
                        Mint.logException(retrofitError);
                        syncResult.stats.numIoExceptions++;
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Failed to add signals: " + error);
                Toast.makeText(context, "Sync failed(signals).", Toast.LENGTH_SHORT).show();
                Mint.logException(error);
                syncResult.stats.numIoExceptions++;
            }
        });

        ObjectFilePair[] signalLogSets = signalLog.getSignalLogObjects();
        for (final ObjectFilePair currLogSet : signalLogSets) {
            if (currLogSet == null) {
                continue;
            }

            service.addSignalsDetails(currLogSet.getJson(), new Callback<JsonObject>() {
                @Override
                public void success(JsonObject integer, Response response) {
                    Log.i(TAG, "Successfully added signals details to " + integer + " stations.");
                    eventLogger.logEvent("Successfully added signal details to " + integer + " stations.");
                    signalLog.clearFile(currLogSet.getFileName());
                    syncResult.stats.numUpdates++;
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(TAG, "Failed to add signals Details: " + error);
                    Toast.makeText(context, "Sync failed(signalsDetails).", Toast.LENGTH_SHORT).show();
                    Mint.logException(error);
                    syncResult.stats.numIoExceptions++;
                }
            });
        }
    }

}

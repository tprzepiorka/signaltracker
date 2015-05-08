package tprz.signaltracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Interface to allow interaction with the SigTrackWeb API which provides
 * access to new tube graph information and a way to upload the results from
 * the app to the web server.
 */
public interface SigTrackWebService {
    @POST("/api/things/signals")
    void addSignals(@Body JsonElement signalData, Callback<JsonObject> callback);

    @POST("/api/things/station")
    void addStations(@Body JsonElement stationData, Callback<JsonObject> callback);

    @GET("/api/things/tubegraph")
    void getLatestTubeGraph(Callback<JsonElement> callback);
}

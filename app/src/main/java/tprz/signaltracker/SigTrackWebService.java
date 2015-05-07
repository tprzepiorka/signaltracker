package tprz.signaltracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;

/**
 * Created by tomprz on 07/05/2015.
 */
public interface SigTrackWebService {
    @POST("/api/things/signals")
    void addSignals(@Body JsonElement signalData, Callback<JsonObject> callback);

    @POST("/api/things/station")
    void addStations(@Body JsonElement stationData, Callback<JsonObject> callback);

    @GET("/api/things/tubegraph")
    void getLatestTubeGraph(Callback<JsonElement> callback);
}

package tprz.signaltracker;

import android.content.Context;

import com.google.gson.JsonObject;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.splunk.mint.Mint;

import org.json.JSONObject;

/**
 * Created by tomprz on 15/05/2015.
 */
public class EventLogger {

    private final Context context;
    private final MixpanelAPI mixpanel;
    private static EventLogger instance;
    public static final String MIXPANEL_TOKEN = "39c031666be76f621a1fbfcc407840bd";

    private EventLogger(Context context) {
        this.context = context;
        this.mixpanel =
                MixpanelAPI.getInstance(context, MIXPANEL_TOKEN);
    }

    public static EventLogger getInstance(Context context) {
        if (instance == null) {
            instance = new EventLogger(context);
        }

        return instance;
    }

    public void logEvent(String event) {
        logEvent(event, new JSONObject());

    }

    public void logEvent(String event, JSONObject json) {
        Mint.logEvent(event);
        mixpanel.track(event, json);
    }

}

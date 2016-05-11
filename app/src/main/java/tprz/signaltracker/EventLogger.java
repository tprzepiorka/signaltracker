package tprz.signaltracker;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.splunk.mint.Mint;

import org.json.JSONObject;

public class EventLogger {

    private final MixpanelAPI mixpanel;
    private static EventLogger instance;
    public static final String MIXPANEL_TOKEN = ""; // Mixpanel token here

    private EventLogger(Context context) {
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

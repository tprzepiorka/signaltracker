package tprz.signaltracker;

import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.WPSStreetAddressLookup;
import com.skyhookwireless.wps.XPS;

import java.util.concurrent.ConcurrentLinkedQueue;

import edu.mit.media.funf.probe.Probe;

/**
 * SkyhookProbe is a probe that uses the skyhook (http://www.skyhookwireless.com/)
 * API to get offline location tokens which can later be looked up.
 * Created by Thomas on 04-Dec-14.
 */
public class SkyhookProbe extends Probe.Base implements Probe.PassiveProbe {
    private XPS _xps;
    private String offlineKey = "mysecretkey";
    private ConcurrentLinkedQueue<OfflineKeyInfo> offlineTokenInfos;

    WPSLocationCallback callback = new WPSLocationCallback()
    {
        boolean hasSentData = false;
        // What the application should do after it's done

        @Override
        public void done() {
            // nothing to do
            Log.d("skyhookprobe", "done() in skyhookapi");
            stop();
        }

        // What the application should do if an error occurs
        @Override
        public WPSContinuation handleError(WPSReturnCode error)
        {
            final byte[] key = Base64.decode(offlineKey, Base64.DEFAULT);
            final byte[] token = _xps.getOfflineToken(null, key);

            saveOfflineData(token);

            // To retry the location call on error use WPS_CONTINUE,
            // otherwise return WPS_STOP
            return WPSContinuation.WPS_STOP;
        }

        // Implements the actions using the location object
        @Override
        public void handleWPSLocation(WPSLocation location)
        {
            hasSentData = false;
            final byte[] key = Base64.decode(offlineKey, Base64.DEFAULT);
            final byte[] token = _xps.getOfflineToken(null, key);
            saveOfflineData(token);

            // you'll implement printLocation()

            Log.d("skyhookProbe", "Got location: " + location.getLatitude() + ", " + location.getLongitude() + ": " + location.getStreetAddress());
        }

        private void saveOfflineData(byte[] token) {
            long currTime = (System.currentTimeMillis() / 1000L);

            String tokenString = token != null ? Base64.encodeToString(token, Base64.DEFAULT) : "Error";
            OfflineKeyInfo tokenInfo = new OfflineKeyInfo(tokenString, currTime);
            offlineTokenInfos.add(tokenInfo);

            JsonObject jo = new JsonObject();
            jo.addProperty("token", tokenInfo.getOfflineToken());
            jo.addProperty("timestamp", currTime);

            JsonObject tokenInfoJson = getGson().toJsonTree(tokenInfo.toMap()).getAsJsonObject();
            Log.d("skyhookProbe", "Saving offline data: " + tokenInfo.getOfflineToken());
            sendData(jo);
            hasSentData = true;

        }

    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("skyhookProbe", "onStart()");
            _xps.getLocation(null,
                    WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP,
                    callback);

//        int count = 0;
//        JsonObject skyhookTokenData = new JsonObject();
//        while(!offlineTokenInfos.isEmpty()) {
//            OfflineKeyInfo tokenInfo = offlineTokenInfos.poll();
//            JsonObject tokenInfoJson = getGson().toJsonTree(tokenInfo).getAsJsonObject();
//            skyhookTokenData.add("" + count++, tokenInfoJson);
//        }
//
//        if(count > 0) {
//            Log.v("SkyhookProbe", "Sending " + count + " skyhook offline tokens.");
//            sendData(skyhookTokenData);
//        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        if(_xps == null) {
            _xps = new XPS(getContext());
            // set the API key
            _xps.setKey("eJwz5DQ0AAEjCzMLzmpXI0NXI3MDM11TJyDhauhopGtgaeGsa2Jp7ORi5GJpZmpiWAsADhMK9A");
        }

        if(offlineTokenInfos == null) {
            offlineTokenInfos = new ConcurrentLinkedQueue<OfflineKeyInfo>();
        }
    }



}

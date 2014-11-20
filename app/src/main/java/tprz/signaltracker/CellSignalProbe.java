package tprz.signaltracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.util.HashMap;

import edu.mit.media.funf.probe.Probe;

/**
 * Created by Thomas on 19-Nov-14.
 */
public class CellSignalProbe extends Probe.Base  {
    private int latestGsmSignalStrength = -1;
    TelephonyManager manager;
    MyPhoneStateListener myListener = new MyPhoneStateListener();

    @Override
    protected void onStart() {
        super.onStart();
        manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        MyPhoneStateListener myListener = new MyPhoneStateListener();
        manager.listen(myListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        //sendData(getGson().toJsonTree(getData()).getAsJsonObject());
        //stop();
    }

    @Override
    protected void onEnable() {
     //   super.onStart();
//        sendData(getGson().toJsonTree(getData()).getAsJsonObject());
        //stop();
    }

    private HashMap<String, String> getData() {
        HashMap<String, String> map = new HashMap<String, String>();
        ConnectivityManager cm =
                (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        String typeName = activeNetworkInfo.getTypeName();
        String subTypeName = activeNetworkInfo.getSubtypeName();

        map.put("signalStrength", "" + latestGsmSignalStrength);
        map.put("connectionType", typeName);
        map.put("connectionSubType", subTypeName);

        return map;
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        public int signalStrengthInt =0;
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength){
            super.onSignalStrengthsChanged(signalStrength);
            latestGsmSignalStrength  = signalStrength.getGsmSignalStrength();

            HashMap<String, String> map = new HashMap<String, String>();
            ConnectivityManager cm =
                    (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            String typeName = activeNetworkInfo.getTypeName();
            String subTypeName = activeNetworkInfo.getSubtypeName();

            map.put("signalStrength", "" + latestGsmSignalStrength);
            map.put("connectionType", typeName);
            map.put("connectionSubType", subTypeName);

            sendData(getGson().toJsonTree(map).getAsJsonObject());
        }

    };


}

package tprz.signaltracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.List;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by Thomas on 19-Nov-14.
 */
@Probe.DisplayName("CellSignal")
@Schedule.DefaultSchedule(interval=10)
public class CellSignalProbe extends Probe.Base  {
    private int latestGsmSignalStrength = -1;
    TelephonyManager telephonyManager;
    MyPhoneStateListener myListener = new MyPhoneStateListener();
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onStart() {
        super.onStart();
        if(telephonyManager == null) {
            telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            MyPhoneStateListener myListener = new MyPhoneStateListener();
            telephonyManager.listen(myListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();

        //sendData(getGson().toJsonTree(getData()).getAsJsonObject());
        //stop();
        if(wakeLock == null) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "call_lock");
        }
        wakeLock.acquire();
    }
//
//    @Override
//    protected void onEnable() {
//        if(wakeLock != null && !wakeLock.isHeld()) {
//            wakeLock.acquire();
//        }
//
//        // Acquire wake lock
//
//    }

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
            String typeName;
            String subTypeName;

            if(activeNetworkInfo != null) {
                typeName = activeNetworkInfo.getSubtypeName();
                subTypeName = activeNetworkInfo.getTypeName();
            } else {
                typeName = "null";
                subTypeName = "null";
            }

            map.put("signalStrength", "" + latestGsmSignalStrength);
            map.put("connectionType", typeName);
            int cdma = signalStrength.getCdmaDbm();
            int evdo = signalStrength.getEvdoDbm();
            map.put("connectionSubType", subTypeName);

            CellLocation infos = telephonyManager.getCellLocation();

            sendData(getGson().toJsonTree(map).getAsJsonObject());
            if(wakeLock != null && wakeLock.isHeld()) {
              // wakeLock.release();
            }
        }


    };


}

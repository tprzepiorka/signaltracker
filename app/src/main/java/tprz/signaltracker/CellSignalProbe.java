package tprz.signaltracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * CellSignalProbe is a probe to measure the cell signal strength of a mobile device.
 * Created by Thomas on 19-Nov-14.
 */
@Probe.DisplayName("CellSignal")
@Schedule.DefaultSchedule(interval=10)
public class CellSignalProbe extends Probe.Base implements Probe.PassiveProbe {

    // True if this device we are running supports the getAllCellInfo() API.
    private boolean supportsCellInfo;
    @SuppressWarnings("FieldCanBeLocal")
    private int latestGsmSignalStrength = -1;
    private TelephonyManager telephonyManager;
    @SuppressWarnings("FieldCanBeLocal")
    private MyPhoneStateListener myListener;
    private PowerManager.WakeLock wakeLock;


    @Override
    protected void onStart() {
        super.onStart();

        if(supportsCellInfo) {
            Log.i("CelSignalProbe", "Using getAllCellInfo()");

            List<Map<String, String>> maps = getData();
            JsonObject cellInfoData = new JsonObject();
            int count = 0;
            for(Map<String, String> map : maps) {
                JsonObject cellInfo = getGson().toJsonTree(map).getAsJsonObject();
                cellInfoData.add("" + count++, cellInfo);
                Log.v("CelSignalProbe", "Adding cellInfo Data from getAllCellInfo().");
            }
            Log.v("CelSignalProbe", "Sending getAllCellInfo() data.");
            sendData(cellInfoData);

            stop();
        } else {
            Log.i("CelSignalProbe", "Using cellSignalChangedListener()");
            // Setup wake lock
            if(wakeLock == null) {
                PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                //noinspection deprecation
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "call_lock");
            }
            wakeLock.acquire();

            // Use Listener
            myListener = new MyPhoneStateListener();
            telephonyManager.listen(myListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

    }

    @Override
    protected void onEnable() {
        Log.v("CelSignalProbe", "onEnable().");
        super.onEnable();

        // Initialise telephony manager
        if(telephonyManager == null) {
            telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        }

        supportsCellInfo = telephonyManager.getAllCellInfo() != null;
    }

    @Override
    protected void onDisable() {
        Log.v("CelSignalProbe", "onDisable().");
        super.onDisable();

        if(wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Use getAllCellInfo to get all CellInfos.
     * @return A list of maps where each map is a type of cellInfo.
     */
    private List<Map<String, String>> getData() {
        List<Map<String, String>> cellInfos = new ArrayList<Map<String, String>>();

        List<CellInfo> allCellInfo = telephonyManager.getAllCellInfo();
        if(allCellInfo == null) {
            Log.w("CellSignalProbe", "allCellInfo is null. SupportsCellInfo: " + supportsCellInfo + ".");
            return cellInfos;
        }

        for(CellInfo cellInfo : allCellInfo) {
            Map<String, String> map = new HashMap<String, String>();
            addCellInfo(map, cellInfo);
            cellInfos.add(map);
        }

        return cellInfos;
    }

    public void addCellInfo(Map<String, String> map, CellInfo cellInfo) {
        if(cellInfo instanceof CellInfoGsm) {
            addCellInfo(map, (CellInfoGsm)cellInfo);
        } else if(cellInfo instanceof CellInfoWcdma) {
            addCellInfo(map, (CellInfoWcdma)cellInfo);
        } else if(cellInfo instanceof CellInfoLte) {
            addCellInfo(map, (CellInfoLte)cellInfo);
        } else if(cellInfo instanceof CellInfoCdma) {
            addCellInfo(map, (CellInfoCdma)cellInfo);
        } else {
            Log.w("CelSignalProbe", "Unsupported type of cellInfo.");
        }
    }

    public void addCellInfo(Map<String, String> map, CellInfoGsm cellInfo) {
        CellIdentityGsm identity = cellInfo.getCellIdentity();
        CellSignalStrengthGsm signalStrength = cellInfo.getCellSignalStrength();

        map.put("cellType", "Gsm");
        map.put("isRegistered", "" + cellInfo.isRegistered());
        map.put("asu", "" + signalStrength.getAsuLevel());
        map.put("dbm", "" + signalStrength.getDbm());
        map.put("level", "" + signalStrength.getLevel());
        map.put("cid", "" + identity.getCid());
        map.put("lac", "" + identity.getLac());
        map.put("mcc", "" + identity.getMcc());
        map.put("mnc", "" + identity.getMnc());
    }

    public void addCellInfo(Map<String, String> map, CellInfoWcdma cellInfo) {
        CellIdentityWcdma identity = cellInfo.getCellIdentity();
        CellSignalStrengthWcdma signalStrength = cellInfo.getCellSignalStrength();

        map.put("cellType", "Wcdma");
        map.put("isRegistered", "" + cellInfo.isRegistered());
        map.put("asu", "" + signalStrength.getAsuLevel());
        map.put("dbm", "" + signalStrength.getDbm());
        map.put("level", "" + signalStrength.getLevel());
        map.put("cid", "" + identity.getCid());
        map.put("lac", "" + identity.getLac());
        map.put("mcc", "" + identity.getMcc());
        map.put("mnc", "" + identity.getMnc());
        map.put("psc", "" + identity.getPsc());
    }

    public void addCellInfo(Map<String, String> map, CellInfoLte cellInfo) {
        CellIdentityLte identity = cellInfo.getCellIdentity();
        CellSignalStrengthLte signalStrength = cellInfo.getCellSignalStrength();

        map.put("cellType", "Lte");
        map.put("isRegistered", "" + cellInfo.isRegistered());
        map.put("asu", "" + signalStrength.getAsuLevel());
        map.put("dbm", "" + signalStrength.getDbm());
        map.put("level", "" + signalStrength.getLevel());
        map.put("timingAdvance", "" + signalStrength.getTimingAdvance());
        map.put("ci", "" + identity.getCi());
        map.put("tac", "" + identity.getTac());
        map.put("mcc", "" + identity.getMcc());
        map.put("mnc", "" + identity.getMnc());
        map.put("pci", "" + identity.getPci());
    }

    public void addCellInfo(Map<String, String> map, CellInfoCdma cellInfo) {
        CellIdentityCdma identity = cellInfo.getCellIdentity();
        CellSignalStrengthCdma signalStrength = cellInfo.getCellSignalStrength();

        map.put("cellType", "Cdma");
        map.put("isRegistered", "" + cellInfo.isRegistered());
        map.put("asu", "" + signalStrength.getAsuLevel());
        map.put("dbm", "" + signalStrength.getDbm());
        map.put("level", "" + signalStrength.getLevel());
        map.put("networkId", "" + identity.getNetworkId());
    }

    private class MyPhoneStateListener extends PhoneStateListener {

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
            map.put("connectionSubType", subTypeName);

            Log.v("CelSignalProbe", "Sending data using onSignalStrengthsChanged");
            sendData(getGson().toJsonTree(map).getAsJsonObject());
        }


    }


}

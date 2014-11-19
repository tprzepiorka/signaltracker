package tprz.signaltracker;

import android.content.Context;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.util.List;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

/**
 * Created by Thomas on 19-Nov-14.
 */
public class CellSignalProbe extends Probe.Base  {
    @Override
    protected void onStart() {
        super.onStart();
        sendData(getGson().toJsonTree(getData()).getAsJsonObject());
        stop();
    }

    private Bundle getData() {
        TelephonyManager manager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cellInfos = manager.getAllCellInfo();
        CellInfoGsm cellInfoGsm = (CellInfoGsm)manager.getAllCellInfo().get(0);
        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfoGsm.getCellSignalStrength();
        int cellDbm = cellSignalStrengthGsm.getDbm();

        Bundle data = new Bundle();
        data.putInt("dbm", cellDbm);
//        data.putInt(TYPE, cellSignalStrengthGsm.getLevel());

        return data;
    }


}

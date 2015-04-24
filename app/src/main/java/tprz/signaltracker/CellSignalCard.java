package tprz.signaltracker;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;

/**
 * This class is a specific Card that is used to display updating information
 * about the phone's current cell signal strength. It updates a thumbnail and text
 * displaying information about the current connection state.
 */
public class CellSignalCard extends Card {
    CellSignalListener cellSignalListener;
    private TextView cellSignalTextView;
    private int[] cellSignalDrawables = new int[] {
            R.drawable.ic_signal_cellular_0_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_1_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_2_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_3_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_4_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_null_grey600_48dp
    };

    public CellSignalCard(Context context, int innerLayout) {
        super(context, innerLayout);

        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        this.cellSignalListener = new CellSignalListener();
        telephonyManager.listen(cellSignalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    /**
     * Updates the card view to display the relevant thumbnail for the given
     * signal strength.
     * @param signalStrength The SignalStrength in ASU
     * @param gsm True if the phone is on a GSM network, false if it is on CDMA
     */
    public void setSignal(int signalStrength, boolean gsm) {
        String contentsText = String.format("%s\nASU: %d", gsm ? "GSM" : "CDMA", signalStrength);
        cellSignalTextView.setText(contentsText);

        int iconLevel;
        if(signalStrength == 0 || signalStrength == 99) {
            iconLevel = 0;
        } else if(signalStrength >= 12) {
            iconLevel = 4;
        } else if(signalStrength >= 8) {
            iconLevel = 3;
        } else if(signalStrength >= 5) {
            iconLevel = 2;
        } else {
            iconLevel = 1;
        }

        CardThumbnail thumbnail = new CardThumbnail(getContext());
        thumbnail.setDrawableResource(cellSignalDrawables[iconLevel]);
        addCardThumbnail(thumbnail);
        notifyDataSetChanged();
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        cellSignalTextView = (TextView) parent.findViewById(R.id.cell_signal_text);
    }

    public class CellSignalListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength){
            setSignal(signalStrength.getGsmSignalStrength(), signalStrength.isGsm());
        }
    }

}

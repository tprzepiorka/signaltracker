package tprz.signaltracker;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;

import it.gmariotti.cardslib.library.internal.Card;
import tprz.signaltracker.location.Station;
import tprz.signaltracker.location.StationCard;

/**
 * The StationLocationCard provides a card to display information
 * about our current position/station as well as the previous station.
 */
public class StationLocationCard extends Card implements StationCard {
    private TextView prevStationText;
    private TextView currStationText;

    private Station lastStation = null;

    public StationLocationCard(Context context, int innerLayout) {
        super(context, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        prevStationText = (TextView) parent.findViewById(R.id.location_prev_text);
        currStationText = (TextView) parent.findViewById(R.id.location_curr_text);
    }

    @Override
    public void updateCard(Station newCurrentStation) {
        if(newCurrentStation != null && !newCurrentStation.equals(lastStation)) {
            prevStationText.setText("Previous: " + (lastStation == null ? "Unknown" : lastStation.getName()));
            lastStation = newCurrentStation;
        }

        currStationText.setText((newCurrentStation == null ? "Unknown" : newCurrentStation.getName()));
    }

}

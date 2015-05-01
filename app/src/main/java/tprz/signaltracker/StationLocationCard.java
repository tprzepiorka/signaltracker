package tprz.signaltracker;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.SearchView;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.Card;
import tprz.signaltracker.location.LocationFingerprint;
import tprz.signaltracker.location.Station;
import tprz.signaltracker.location.StationCard;

/**
 * The StationLocationCard provides a card to display information
 * about our current position/station as well as the previous station.
 */
public class StationLocationCard extends Card implements StationCard {
    private TextView prevStationText;
    private TextView currStationText;
    private AutoCompleteTextView autoCompleteTextView;

    private Station lastStation = null;
    private LocationFingerprint currLocFingerprint = null;

    public StationLocationCard(Context context, int innerLayout) {
        super(context, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        prevStationText = (TextView) parent.findViewById(R.id.location_prev_text);
        currStationText = (TextView) parent.findViewById(R.id.location_curr_text);
        autoCompleteTextView = (AutoCompleteTextView) parent.findViewById(R.id.autoCompleteTextView);

    }

    @Override
    public void updateCard(Station newCurrentStation, LocationFingerprint locFingerprint) {
        if(newCurrentStation != null && !newCurrentStation.equals(lastStation)) {
            prevStationText.setText("Previous: " + (lastStation == null ? "Unknown" : lastStation.getName()));
            lastStation = newCurrentStation;
        }

        currStationText.setText((newCurrentStation == null ? "Unknown" : newCurrentStation.getName()));
        currLocFingerprint = locFingerprint;
    }

}

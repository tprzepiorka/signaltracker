package tprz.signaltracker;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.Card;
import tprz.signaltracker.location.LocationFingerprint;
import tprz.signaltracker.location.LocationProvider;
import tprz.signaltracker.location.Station;
import tprz.signaltracker.location.StationCard;
import tprz.signaltracker.location.TubeGraph;
import tprz.signaltracker.location.WifiProfiler;

/**
 * The StationLocationCard provides a card to display information
 * about our current position/station as well as the previous station.
 */
public class StationLocationCard extends Card implements StationCard, LocationProvider {
    private final Activity activity;
    private final TubeGraph tubeGraph;
    private final WifiProfiler wifiProfiler;
    private TextView prevStationText;
    private TextView currStationText;
    private AutoCompleteTextView autoCompleteTextView;
    private ImageButton saveIdentificationButton;
    @SuppressWarnings("UnusedDeclaration")
    private final String TAG = "StationLocationCard";

    private Station lastStation = null;
    private LocationFingerprint currLocFingerprint = null;
    private Station currStation;

    public StationLocationCard(Context context, int innerLayout, Activity activity, TubeGraph tubeGraph) {
        super(context, innerLayout);
        this.activity = activity;
        this.tubeGraph = tubeGraph;
        this.wifiProfiler = new WifiProfiler(getContext(), this, tubeGraph);
        wifiProfiler.startWifiScan();
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        prevStationText = (TextView) parent.findViewById(R.id.location_prev_text);
        currStationText = (TextView) parent.findViewById(R.id.location_curr_text);
        autoCompleteTextView = (AutoCompleteTextView) parent.findViewById(R.id.autoCompleteTextView);
        saveIdentificationButton = (ImageButton) parent.findViewById(R.id.imageButton);
        saveIdentificationButton.setEnabled(false);
        saveIdentificationButton.setClickable(false);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int pos,
                                    long id) {
                saveIdentificationButton.setEnabled(true);
                saveIdentificationButton.setClickable(true);
            }
        });


        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            String match = null;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                match = null;
                String[] stationNames = getContext().getResources().getStringArray(R.array.stations);
                for(String station : stationNames) {
                    if(station.equalsIgnoreCase(s.toString().trim()) && !station.equals(s.toString())) {
                        match = station;
                    }
                }
                saveIdentificationButton.setEnabled(false);
                saveIdentificationButton.setClickable(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(match != null) {
                    s.replace(0, Math.max(s.length(), match.length()), match);
                    match = null;

                    saveIdentificationButton.setEnabled(true);
                    saveIdentificationButton.setClickable(true);
                }
            }
        });

        autoCompleteTextView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER) {
                    newStationSubmitted();
                    return true;
                }

                return false;
            }
        });

        saveIdentificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newStationSubmitted();
            }
        });

        String[] stationNames = getContext().getResources().getStringArray(R.array.stations);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, stationNames);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);
    }

    private void newStationSubmitted() {
        if(currLocFingerprint != null) {
            // Find the station selected
            String selectedStationName = autoCompleteTextView.getText().toString();
            Station station = tubeGraph.getStationByName(selectedStationName);
            if(station != null) {
                if(currLocFingerprint.mapNewStation(station)) {
                    currStationText.setText(activity.getString(R.string.addingStationText));
                }
                wifiProfiler.startWifiScan();
                autoCompleteTextView.setText("");
            }
        }
    }

    @Override
    public void updateCard(Station newCurrentStation, LocationFingerprint locFingerprint) {
        if(newCurrentStation != null && !newCurrentStation.equals(lastStation)) {
            prevStationText.setText(String.format(activity.getString(R.string.previousStationTextPrepend),
                    (lastStation == null
                            ? activity.getString(R.string.unknownStationText)
                            : lastStation.getName())));
            lastStation = newCurrentStation;
        }

        currStationText.setText((newCurrentStation == null
                ? activity.getString(R.string.unknownStationText)
                : newCurrentStation.getName()));
        currStation = newCurrentStation;

        currLocFingerprint = locFingerprint;
    }

    public Station getCurrentStation() {
        return currStation;
    }

}

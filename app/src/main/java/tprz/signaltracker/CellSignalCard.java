package tprz.signaltracker;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import tprz.signaltracker.location.Station;

/**
 * This class is a specific Card that is used to display updating information
 * about the phone's current cell signal strength. It updates a thumbnail and text
 * displaying information about the current connection state.
 */
public class CellSignalCard extends Card {
    CellSignalListener cellSignalListener;
    private TextView cellSignalTextView;
    private TextView prevStationText;
    private TextView currStationText;
    private LineChart chart;
    private int chartWidth = 30;
    private int[] cellSignalDrawables = new int[] {
            R.drawable.ic_signal_cellular_0_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_1_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_2_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_3_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_4_bar_grey600_48dp,
            R.drawable.ic_signal_cellular_null_grey600_48dp
    };
    private Station lastStation = null;

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

        // Charting
        updateChart(signalStrength);
    }

    private void initChart() {
        chart.setVisibleXRange(50);
        chart.getAxis(YAxis.AxisDependency.LEFT).setAxisMinValue(0);
        chart.getAxis(YAxis.AxisDependency.LEFT).setAxisMaxValue(32);
        chart.getAxis(YAxis.AxisDependency.LEFT).setLabelCount(2);
        chart.getAxis(YAxis.AxisDependency.RIGHT).setEnabled(false);
        chart.setVisibleYRange(32, YAxis.AxisDependency.RIGHT);
        chart.getXAxis().setDrawGridLines(false);

        ArrayList<String> x = new ArrayList<>();
        List<Entry> y = new ArrayList<>();
        for(int i =0; i<30;i++) {
            x.add("" + i);
            y.add(new Entry(0, i));
        }
        LineDataSet dataSet = new LineDataSet(y, "ASU");
        LineData lineData = new LineData(x, dataSet);
        chart.setData(lineData);
    }

    /***
     *
     * http://stackoverflow.com/questions/28105547/how-to-make-a-realtime-rollingwindow-graph-using-mpandroidchart
     * @param signalStrength
     */
    private void updateChart(int signalStrength) {
        LineData lineData = chart.getData();
        LineDataSet lineDataSet = lineData.getDataSetByIndex(0);
        int count = lineDataSet.getEntryCount();


        if (lineData.getXValCount() <= count) {
            // Remove/Add XVal
//            lineData.getXVals().add("" + count);
//            lineData.getXVals().remove(0);

            // Move all entries 1 to the left..
            for (int i=0; i < count; i++) {
                Entry e = lineDataSet.getEntryForXIndex(i);
                if (e==null) continue;

                e.setXIndex(e.getXIndex() - 1);
            }

            // Set correct index to add value
            count = chartWidth;
        }

        // Add new value
        lineData.addEntry(new Entry(signalStrength, count), 0);

        // Make sure to draw
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        cellSignalTextView = (TextView) parent.findViewById(R.id.cell_signal_text);
        prevStationText = (TextView) parent.findViewById(R.id.location_prev_text);
        currStationText = (TextView) parent.findViewById(R.id.location_curr_text);
        this.chart = (LineChart) parent.findViewById(R.id.chart);
        initChart();
    }

    public void updateCard(Station newCurrentStation) {
        if(newCurrentStation != null && !newCurrentStation.equals(lastStation)) {
            prevStationText.setText("Previous: " + (lastStation == null ? "Unknown" : lastStation.getName()));
            lastStation = newCurrentStation;
        }

        currStationText.setText("At : " + (newCurrentStation == null ? "Unknown" : newCurrentStation.getName()));
    }

    public class CellSignalListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength){
            setSignal(signalStrength.getGsmSignalStrength(), signalStrength.isGsm());

            MultiLogger.log(TAG, String.format("(signalStrength, %d)", signalStrength.getGsmSignalStrength()));
        }
    }

}

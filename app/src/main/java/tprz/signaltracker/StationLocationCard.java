package tprz.signaltracker;

import android.content.Context;

import it.gmariotti.cardslib.library.internal.Card;
import tprz.signaltracker.location.Station;

/**
 * Created by tomprz on 30/04/2015.
 */
public class StationLocationCard extends Card {

    public StationLocationCard(Context context) {
        super(context);
    }

    public StationLocationCard(Context context, int innerLayout) {
        super(context, innerLayout);
    }


    public void updateCard(Station station) {

    }
}

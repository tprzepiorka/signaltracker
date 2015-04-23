package tprz.signaltracker;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;

/**
 * Created by tomprz on 23/04/2015.
 */
public class CellSignalCard extends Card {
    private TextView cellSignalTextView;

    public CellSignalCard(Context context) {
        super(context);
    }

    public CellSignalCard(Context context, int innerLayout) {
        super(context, innerLayout);
    }

    public void setSignal(int signalStrength) {
         cellSignalTextView.setText("" + signalStrength);
        if(signalStrength > 8) {
            CardThumbnail thumbnail = new CardThumbnail(getContext());
            thumbnail.setDrawableResource(R.drawable.ic_signal_cellular_2_bar_white_24dp);
            addCardThumbnail(thumbnail);
            notifyDataSetChanged();
        }
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        cellSignalTextView = (TextView) parent.findViewById(R.id.cell_signal_text);
    }

}

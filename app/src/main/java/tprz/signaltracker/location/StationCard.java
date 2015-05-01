package tprz.signaltracker.location;

/**
 * A card that can be updated with new Station information
 */
public interface StationCard {

    /**
     * Update the card to reflect a change in the current Station
     * @param newCurrentStation The current Station we are at
     */
    public void updateCard(Station newCurrentStation);

}

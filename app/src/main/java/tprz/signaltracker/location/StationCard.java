package tprz.signaltracker.location;

/**
 * A card that can be updated with new Station information
 */
public interface StationCard {

    /**
     * Update the card to reflect a change in the current Station
     * @param newCurrentStation The current Station we are at
     * @param locFingerprint
     */
    public void updateCard(Station newCurrentStation, LocationFingerprint locFingerprint);

}

package tprz.signaltracker.location;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a station in the tube network that has connections to other stations and
 * other properties. Provides some methods for exploring the nearby stations and map
 */
public class Station {
    String name;
    List<Edge> east;
    List<Edge> west;
    Set<String> ssids;
    Map<String, Double> predictedSignalStrengths;

    public Station(String name, Map<String, Double> predictedSignalStrengths, Set<String> ssids) {
        this.name = name;
        this.predictedSignalStrengths = predictedSignalStrengths;
        this.ssids = ssids;
    }

    public int getNextGoodStationTime(WifiProfiler.Direction direction, int threshold,
                                      String networkProvider) {
        return getNextGoodStationTime(direction, threshold, 0, networkProvider);
    }

    public int getNextGoodStationTime(WifiProfiler.Direction direction, int threshold,
                                      int timeSoFar, String networkProvider) {
        List<Edge> edges;
        if(direction.equals(WifiProfiler.Direction.EAST)) {
            edges = east;
        } else if(direction.equals(WifiProfiler.Direction.WEST)) {
            edges = west;
        } else {
            return -1;
        }

        //noinspection LoopStatementThatDoesntLoop
        for(Edge edge : edges) {
            if(edge.end.predictedSignalStrengths.containsKey(networkProvider) &&
                    edge.end.predictedSignalStrengths.get(networkProvider) > threshold) {
                return timeSoFar + edge.length;
            } else {
                return edge.end.getNextGoodStationTime(direction, threshold, timeSoFar, networkProvider);
            }
        }

        return -1;
    }

    public WifiProfiler.Direction getDirectionFromPrevStation(Station prevStation) {
        for(Edge edge : east) {
            if(edge.end.equals(prevStation)) {
                return WifiProfiler.Direction.WEST;
            }
        }

        for(Edge edge : west) {
            if(edge.end.equals(prevStation)) {
                return WifiProfiler.Direction.EAST;
            }
        }

        // Not one hop away, check recursively
        for(Edge edge : east) {
            WifiProfiler.Direction dir = edge.end.getDirectionFromPrevStation(prevStation);
            if(dir != null) {
                return dir;
            }
        }

        for(Edge edge : west) {
            WifiProfiler.Direction dir = edge.end.getDirectionFromPrevStation(prevStation);
            if(dir != null) {
                return dir;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof Station) {
            Station other = (Station) object;
            return name.equals(other.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}

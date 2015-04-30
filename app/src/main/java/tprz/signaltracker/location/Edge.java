package tprz.signaltracker.location;

/**
 * Directional Edge in a Station map graph
 */
public class Edge{
    Station start;
    Station end;
    int length;

    public Edge(Station start, Station end, int length) {
        this.start = start;
        this.end = end;
        this.length = length;
    }
}

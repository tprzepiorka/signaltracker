package tprz.signaltracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Hold a skyhook offline key and some meta info
 * Created by Thomas on 04-Dec-14.
 */
public class OfflineKeyInfo {

    private String offlineToken;
    private long timestamp;

    public OfflineKeyInfo(String offlineToken, long timestamp) {
        this.offlineToken = offlineToken;
        this.timestamp = timestamp;
    }

    public String getOfflineToken() {
        return offlineToken;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, String> toMap() {
        Map<String,String> map = new HashMap<String, String>();
        map.put("offlineToken", "" + offlineToken);
        map.put("timestamp", "" + timestamp);

        return map;
    }
}

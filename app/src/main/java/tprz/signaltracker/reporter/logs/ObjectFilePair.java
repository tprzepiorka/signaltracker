package tprz.signaltracker.reporter.logs;

import com.google.gson.JsonArray;

/**
 * Pair of a JsonArray and a String file name.
 */
public class ObjectFilePair {
    private JsonArray json;
    private String fileName;

    public ObjectFilePair(JsonArray json, String fileName) {
        this.json = json;
        this.fileName = fileName;
    }


    public JsonArray getJson() {
        return json;
    }

    public String getFileName() {
        return fileName;
    }
}
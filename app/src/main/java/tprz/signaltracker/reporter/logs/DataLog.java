package tprz.signaltracker.reporter.logs;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tprz.signaltracker.location.TubeGraph;

/**
 * A log that can be sent to a server to be reported. Is held in a folder
 * as a series of files.
 */
public class DataLog {
    protected static final long MAX_FILE_SIZE = 500 * 1024; // Bytes
    private static final int MAX_FILES = 400;
    private final String folderName;
    private final String fileName;
    protected String fullFileDir;
    protected int currentFileCount = 0;
    private final String TAG = "DataLog";

    public DataLog(String basePath, String logName) {
        this.folderName = logName;
        this.fileName = logName + "_";
        this.fullFileDir = basePath + "/" + folderName;
        initDirectory(fullFileDir);
        setCurrentFileAmount();
    }

    protected void createNewFile() {
        File fileToCreate = new File(getFile(currentFileCount + 1));
        try {
            if(fileToCreate.createNewFile()) {
                currentFileCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearFile(String path) {
        File file = new File(path);
        boolean deleted = file.delete();
    }

    protected JsonArray getJsonArrayFromFile(String filePath) throws Exception {
        String content = TubeGraph.getStringFromFile(filePath);
        JsonParser parser = new JsonParser();
        return content.isEmpty() ? new JsonArray() : (JsonArray)parser.parse(content);
    }

    protected String getFile(int i) {
        return fullFileDir + "/" + fileName + i + getFileExtension();
    }

    protected String getFileExtension() {
        return ".log";
    }

    protected void setCurrentFileAmount() {
        for(int i = 0; i < MAX_FILES; i++) {
            String filePath = getFile(i);
            if(fileExists(filePath)) {
                currentFileCount = i;

                // Create file
                File fileToCreate = new File(filePath);

                try {
                    fileToCreate.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    protected boolean fileExists(String filePath) {
        File signalReadingsDetailsFile = new File(filePath);

        return signalReadingsDetailsFile.exists();
    }

    protected void initDirectory(String dirPath) {
        File theDir = new File(dirPath);
        theDir.mkdir();
    }

    /**
     * Write json file to a given filepath.
     * @param json json to write.
     * @param filePath path to file to write to.
     * @throws java.io.IOException
     * @throws org.json.JSONException
     */
    protected void writeJson(JsonElement json, String filePath) throws IOException, JSONException {
        FileWriter file = new FileWriter(filePath);
        try {
            file.write(json.toString());

            System.out.println("Successfully Copied JSON Object to File...");

        } catch (IOException e) {
            Log.i(TAG, "Error: " + e);
            e.printStackTrace();
        } finally {
            file.flush();
            file.close();
        }
    }

    public ObjectFilePair[] getSignalLogObjects() {
        List<ObjectFilePair> logs = new ArrayList<>();
        for(int i = 0; i <= currentFileCount; i++) {
            String filePath = getFile(i);
            if(fileExists(filePath)) {
                try {
                    JsonArray item = getJsonArrayFromFile(filePath);
                    logs.add(new ObjectFilePair(item, filePath));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        return logs.toArray(new ObjectFilePair[logs.size()]);
    }

    public void addLogObject(JsonObject log) {
        File file = new File(getFile(currentFileCount));
        if(!fileExists(getFile(currentFileCount))) {
            createNewFile();
            file = new File(getFile(currentFileCount));
        }
        if(file.length() >= MAX_FILE_SIZE) {
            createNewFile();
        }

        try {
            JsonArray signalLogs = getJsonArrayFromFile(getFile(currentFileCount));
            signalLogs.add(log);

            writeJson(signalLogs, getFile(currentFileCount));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

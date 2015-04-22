package tprz.signaltracker;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;


import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * BandwidthProbe downloads a file and estimates the rate at which we have
 * been downloaded data so that we can get an estimated for downlink bandwidth speeds.
 */

@Probe.DisplayName("BandwidthProbe")
@Schedule.DefaultSchedule(interval=10)
public class BandwidthProbe extends Probe.Base{

    private final String TAG = "BandwidthProbe";
    private DownloadManager manager;
    private int previousTimestamp = -1;
    private int previousBytesDownloaded = -1;
    private long downloadId = -1;


    @Override
    protected void onStart() {
        super.onStart();

        JsonObject bandwidthData = getBandwidthInfo();
        if(bandwidthData != null) {
            sendData(bandwidthData);
        }

        stop();
    }



    @Override
    protected void onEnable() {
        // We kick off a download using the DownloadManager and routinely check how much data has been downloaded
        if(manager == null) {
            manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);

            startDownload();
        }
    }

    private void startDownload() {
        String downloadUrl = "http://ipv4.download.thinkbroadband.com/1GB.zip";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

        request.setDescription("Download ubuntu iso");
        request.setTitle("Ubuntu Download");

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.ext");

        // get download service and enqueue file
        downloadId = manager.enqueue(request);
    }

    /***
     * Get the estimated bandwidth speed by checking how much data we have downloaded in
     * the last time interval via the DownloadManager
     * @return A JsonObject that has the download difference, estimated speed, and seconds difference
     */
    private JsonObject getBandwidthInfo() {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = manager.query(query);
        if (c.moveToFirst()) {
            String bytesDownloadedSoFar = c
                    .getString(c
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int status = c
                    .getInt(c
                            .getColumnIndex(DownloadManager.COLUMN_STATUS));
            JsonObject bandwidthData = new JsonObject();
            int timestamp = (int) (System.currentTimeMillis() / 1000L);
            bandwidthData.addProperty("timestamp", timestamp);
            bandwidthData.addProperty("downloadedSoFar", bytesDownloadedSoFar);
            Log.i(TAG, "Data downloaded: " + bytesDownloadedSoFar);
            Log.i(TAG, "Download status: " + status);

            if (previousTimestamp != -1) {
                int downloadedDiff = Integer.parseInt(bytesDownloadedSoFar) - previousBytesDownloaded;
                int secondsDiff = timestamp - previousTimestamp;
                int bytesPerSecond = downloadedDiff / secondsDiff;

                bandwidthData.addProperty("downloadDiff", downloadedDiff);
                bandwidthData.addProperty("secondsDiff", secondsDiff);
                bandwidthData.addProperty("estimatedSpeed", bytesPerSecond);
                Log.i("BandwidthProbe", "Esimated Speed: " + bytesPerSecond / 1024 + " KB/S");
            }

            // Redownload if complete
            if(status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                startDownload();
                Log.i(TAG, "Starting next file download");
            }

            previousTimestamp = timestamp;
            previousBytesDownloaded = Integer.parseInt(bytesDownloadedSoFar);

            return bandwidthData;
        }
        return null;
    }

    public static void cancelDownloads(DownloadManager manager) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING);
        Cursor c = manager.query(query);
        c.moveToFirst();
        while (c.moveToNext()) {
            Long id = c
                    .getLong(c
                            .getColumnIndex(DownloadManager.COLUMN_ID));
            int res = manager.remove(id);
                if(res == 0) {
                    Log.i("BandwidthProbe", "Item was not removed from Download manager");
                } else {
                    Log.i("BandwidthProbe", "Removing download with id: " + id);
                }

        }
    }


}

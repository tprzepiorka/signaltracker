package tprz.signaltracker;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.splunk.mint.Mint;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.HardwareInfoProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.view.CardView;
import it.gmariotti.cardslib.library.view.CardViewNative;
import tprz.signaltracker.location.TubeGraph;


public class MainActivity extends Activity  implements Probe.DataListener{

    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private BasicPipeline pipeline;
    private WifiProbe wifiProbe;
    private CellSignalProbe cellSignalProbe;
    private BandwidthProbe bandwidthProbe;
    private HardwareInfoProbe hardwareInfoProbe;
    private ToggleButton enabledToggle;
    private Button archiveButton, scanNowButton;
    private Button syncButton;
    private TextView dataCountView;
    private Handler handler;
    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder)service).getManager();

            Gson gson = funfManager.getGson();
            wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
            cellSignalProbe = gson.fromJson(new JsonObject(), CellSignalProbe.class);
            bandwidthProbe = gson.fromJson(new JsonObject(), BandwidthProbe.class);
            hardwareInfoProbe = gson.fromJson(new JsonObject(), HardwareInfoProbe.class);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            wifiProbe.registerPassiveListener(MainActivity.this);
            hardwareInfoProbe.registerPassiveListener(MainActivity.this);
            cellSignalProbe.registerPassiveListener(MainActivity.this);
            bandwidthProbe.registerListener(MainActivity.this);


            // This toggle button enables or disables the pipeline
            enabledToggle.setChecked(pipeline.isEnabled());
            enabledToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (funfManager != null) {
                        if (isChecked) {
                            funfManager.enablePipeline(PIPELINE_NAME);
                            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
                        } else {
                            funfManager.disablePipeline(PIPELINE_NAME);
                            BandwidthProbe.cancelDownloads((DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE));
                            stopDownloads();
                        }
                    }

                    MultiLogger.isEnabled = enabledToggle.isEnabled();
                }
            });

            // Set UI ready to use, by enabling buttons
            enabledToggle.setEnabled(true);
            archiveButton.setEnabled(true);
            scanNowButton.setEnabled(true);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mint.initAndStartSession(MainActivity.this, "65ac44bd");
        setContentView(R.layout.main);

        // Displays the count of rows in the data
        dataCountView = (TextView) findViewById(R.id.dataCountText);

        // Used to make interface changes on main thread
        handler = new Handler();

        setupCards();

        enabledToggle = (ToggleButton) findViewById(R.id.enabledToggle);
        enabledToggle.setEnabled(false);

        // Runs an archive if pipeline is enabled
        archiveButton = (Button) findViewById(R.id.archiveButton);
        archiveButton.setEnabled(false);
        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pipeline.isEnabled()) {
                    pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);

                    // Wait 1 second for archive to finish, then refresh the UI
                    // (Note: this is kind of a hack since archiving is seamless and there are no messages when it occurs)
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                            //updateScanCount();
                        }
                    }, 1000L);
                } else {
                    Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Forces the pipeline to scan now
        scanNowButton = (Button) findViewById(R.id.scanNowButton);
        scanNowButton.setEnabled(false);
        scanNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pipeline.isEnabled()) {

                    // Manually register the pipeline
                    wifiProbe.registerListener(pipeline);
//                    locationProbe.registerListener(pipeline);
////                    cellTowerProbe.registerListener(pipeline);
//                    cellSignalProbe.registerListener(pipeline);
                    bandwidthProbe.registerListener(pipeline);
//                    hardwareInfoProbe.registerListener(pipeline);
                } else {
                    Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        syncButton = (Button) findViewById(R.id.sync_button);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataReporter dataReporter = DataReporter.getInstance();
                dataReporter.sync();
            }
        });

        // Bind to the service, to create the connection with FunfManager
        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);

        MultiLogger.isEnabled = enabledToggle.isEnabled();

    }

    private void stopDownloads() {
        DownloadManager manager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
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

    private void setupCards() {

        // Station Location Card
        TubeGraph tubeGraph = new TubeGraph();
        StationLocationCard stationLocationCard = new StationLocationCard(getApplicationContext(), R.layout.card_location_layout, this, tubeGraph);

        CardViewNative stationLocationCardView = (CardViewNative) findViewById(R.id.location_card_view);
       stationLocationCardView.setCard(stationLocationCard);

        //Create a Card
        CellSignalCard cellSignalCard = new CellSignalCard(getApplicationContext(), R.layout.card_inner_layout, stationLocationCard);

        CardThumbnail thumbnail = new CardThumbnail(getApplicationContext());
        thumbnail.setDrawableResource(R.drawable.ic_signal_cellular_null_grey600_48dp);
        cellSignalCard.addCardThumbnail(thumbnail);

        //Create a CardHeader
        CardHeader header = new CardHeader(getApplicationContext());
        header.setTitle("Cell Signal Probe");
        //Add Header to card
        cellSignalCard.addCardHeader(header);

        CardView cardView = (CardView) findViewById(R.id.carddemo);

        cardView.setCard(cellSignalCard);
        cellSignalCard.setSignal(0, true);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataReceived(IJsonObject iJsonObject, IJsonObject iJsonObject2) {

    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        Log.i("MainActivity", "onDataCompleted");
        //updateScanCount();
        // Re-register to keep listening after probe completes.
        wifiProbe.registerPassiveListener(this);
//        locationProbe.registerPassiveListener(this);
//        cellSignalProbe.registerPassiveListener(this);
        bandwidthProbe.registerPassiveListener(this);
//        hardwareInfoProbe.registerPassiveListener(this);
    }

    private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;
    /**
     * Queries the database of the pipeline to determine how many rows of data we have recorded so far.
     */
    private void updateScanCount() {
        // Query the pipeline db for the count of rows in the data table
        SQLiteDatabase db = pipeline.getDb();
        if(db.isOpen()) {
            db.close();
       }
        Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
        mcursor.moveToFirst();
        final int count = mcursor.getInt(0);
        mcursor.close();
        // Update interface on main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCountView.setText("Data Count: " + count);
            }
        });


    }


}

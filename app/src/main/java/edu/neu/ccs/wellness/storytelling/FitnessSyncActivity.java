package edu.neu.ccs.wellness.storytelling;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import edu.neu.ccs.wellness.storytelling.homeview.HomeAdventurePresenter;
import edu.neu.ccs.wellness.storytelling.sync.SyncStatus;
import edu.neu.ccs.wellness.storytelling.utils.StorywellPerson;
import edu.neu.ccs.wellness.storytelling.viewmodel.FitnessSyncViewModel;
import edu.neu.ccs.wellness.trackers.miband2.MiBandScanner;

public class FitnessSyncActivity extends AppCompatActivity {

    private static final String NEW_LINE = "\n";

    private boolean isSyncronizingFitnessData = false;
    private FitnessSyncViewModel fitnessSyncViewModel;
    private SyncStatus fitnessSyncStatus = SyncStatus.UNINITIALIZED;

    private StringBuilder logStringBuilder = new StringBuilder();
    private TextView logTextView;
    private Button syncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitness_sync);

        // Set up the views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        logTextView = findViewById(R.id.log_text);
        syncButton = findViewById(R.id.button_sync);

        // Prepare the variables
        logStringBuilder.append(getString(R.string.fitness_sync_initial_log));

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doToggleSync();
            }
        });
    }

    private void doToggleSync() {
        if (this.isSyncronizingFitnessData) {
            stopFitnessSync();
        } else {
            trySyncFitnessData();
        }
    }

    // FITNESS SYNC METHODS
    /**
     * Start synchronizing fitness data and update the UI elements.
     * If the family is in the demo mode, then synchronization will not happen.
     * @return
     */
    public boolean trySyncFitnessData() {
        if (!MiBandScanner.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, HomeAdventurePresenter.REQUEST_ENABLE_BT);
            return false;
        } else {
            isSyncronizingFitnessData = true;
            initializeFitnessSync();
            startFitnessSync();
            syncButton.setText(R.string.fitness_sync_button_stop);
            return true;
        }
    }

    /**
     * Intialize the fitnessSyncViewModel.
     */
    private void initializeFitnessSync() {
        logProgress("Starting fitness data sync.");
        if (this.fitnessSyncViewModel == null) {
            this.fitnessSyncViewModel = ViewModelProviders.of(this)
                    .get(FitnessSyncViewModel.class);
            this.fitnessSyncViewModel.getLiveStatus().observe(this, new Observer<SyncStatus>() {
                @Override
                public void onChanged(@Nullable SyncStatus syncStatus) {
                    onSyncStatusChanged(syncStatus);
                }
            });
        }
    }

    /**
     * Start the fitness sync process.
     */
    private void startFitnessSync() {
        this.fitnessSyncViewModel.perform();
    }

    /**
     * Stop the fitness sync process.
     */
    private void stopFitnessSync() {
        if (isSyncronizingFitnessData) {
            fitnessSyncViewModel.stop();
            isSyncronizingFitnessData = false;
            syncButton.setText(R.string.fitness_sync_button_sync);
            logProgress("Stopped fitness data sync.");
        }
    }

    /**
     * Event handler on SyncStatus change.
     * @param syncStatus
     */
    private void onSyncStatusChanged(SyncStatus syncStatus) {
        this.fitnessSyncStatus = syncStatus;

        switch (syncStatus) {
            case UNINITIALIZED:
                break;
            case NO_NEW_DATA:
                logProgress("No new data within interval.");
                break;
            case NEW_DATA_AVAILABLE:
                logProgress("New data is available...");
                break;
            case INITIALIZING:
                logProgress("Initializing sync...");
                break;
            case CONNECTING:
                logProgress("Connecting to: " + getCurrentPersonString());
                break;
            case DOWNLOADING:
                logProgress("Downloading fitness data: " + getCurrentPersonString());
                break;
            case UPLOADING:
                logProgress("Uploading fitness data: " + getCurrentPersonString());
                break;
            case IN_PROGRESS:
                logProgress("Sync completed for: " + getCurrentPersonString());
                fitnessSyncViewModel.performNext();
                break;
            case COMPLETED:
                logProgress("Successfully synchronizing all devices.");
                stopFitnessSync();
                break;
            case FAILED:
                logProgress(fitnessSyncViewModel.getErrorMessage());
                logProgress("Synchronization failed");
                stopFitnessSync();
                break;
        }
    }

    /* HELPER METHODS */
    /**
     * Get the name of the person who is currently being synced.
     * @return
     */
    private String getCurrentPersonString() {
        StorywellPerson person = fitnessSyncViewModel.getCurrentPerson();
        if (person != null) {
            return person.toString();
        } else {
            return "Null Person";
        }
    }

    // LOGGER METHODS
    /**
     * Update the logger to show the text.
     * @param logText
     */
    private void logProgress(String logText) {
        logStringBuilder.append(NEW_LINE).append(logText);
        logTextView.setText(logStringBuilder.toString());
    }

}

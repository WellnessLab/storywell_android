package edu.neu.ccs.wellness.storytelling.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

public class FitnessSyncJobService extends JobService
        implements FitnessSync.OnFitnessSyncProcessListener {

    private static final String TAG = "SWELL-SVC";
    private FitnessSync fitnessSync;
    private JobParameters params;

    public FitnessSyncJobService() {
    }

    /* BASE METHODS */
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Starting FitnessSyncJobService");
        UserLogging.logStartBgBleSync();

        Storywell storywell = new Storywell(getApplicationContext());
        this.fitnessSync = new FitnessSync(getApplicationContext(), this);
        this.fitnessSync.perform(storywell.getGroup());

        this.params = params;

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Stopping FitnessSyncJobService");
        return false;
    }

    /* OnFitnessSyncProcessListener METHODS */
    @Override
    public void onSetUpdate(SyncStatus syncStatus) {
        this.handleStatusChange(syncStatus);
    }

    @Override
    public void onPostUpdate(SyncStatus syncStatus) {
        this.handleStatusChange(syncStatus);
    }

    /* SYNC UPDATE METHODS */
    private void handleStatusChange(SyncStatus syncStatus) {
        /*
        if (SyncStatus.CONNECTING.equals(syncStatus)) {
            Log.d(TAG, "Connecting: " + getCurrentPersonString());
        } else if (SyncStatus.DOWNLOADING.equals(syncStatus)) {
            Log.d(TAG, "Downloading fitness data: " + getCurrentPersonString());
        } else if (SyncStatus.UPLOADING.equals(syncStatus)) {
            Log.d(TAG, "Uploading fitness data: " + getCurrentPersonString());
        } else if (SyncStatus.IN_PROGRESS.equals(syncStatus)) {
            String msg = "Sync completed for: " + getCurrentPersonString();
            Log.d(TAG, msg);
            UserLogging.logBgBleInfo(msg);
            this.fitnessSync.performNext();
        } else if (SyncStatus.COMPLETED.equals(syncStatus)) {
            completeSync();
            Log.d(TAG, "All sync successful!");
            UserLogging.logStopBgBleSync(true);
        } else if (SyncStatus.FAILED.equals(syncStatus)) {
            completeSync();
            Log.d(TAG, "Sync failed");
            UserLogging.logStopBgBleSync(false);
        }
        */
        switch (syncStatus) {
            case CONNECTING:
                Log.d(TAG, "Connecting: " + getCurrentPersonString());
                break;
            case DOWNLOADING:
                Log.d(TAG, "Downloading fitness data: " + getCurrentPersonString());
                break;
            case UPLOADING:
                Log.d(TAG, "Uploading fitness data: " + getCurrentPersonString());
                break;
            case IN_PROGRESS:
                String msg = "Sync completed for: " + getCurrentPersonString();
                Log.d(TAG, msg);
                UserLogging.logBgBleInfo(msg);
                this.fitnessSync.performNext();
                break;
            case COMPLETED:
                Log.d(TAG, "All sync successful!");
                UserLogging.logStopBgBleSync(true);
                completeSync();
                break;
            case FAILED:
                if (this.isJobRunning) {
                    Log.d(TAG, "Sync failed");
                    UserLogging.logStopBgBleSync(false);
                }
                completeSync();
                break;

        }
    }

    private void completeSync() {
        Log.d(TAG, "Stopping sync service");
        this.scheduleSync();
        // this.stopSelf();
        this.fitnessSync.stop();
        this.jobFinished(params, false);
    }

    private void scheduleSync() {
        int triggerAtMillis = 60 * 60 * 1000;
        FitnessSyncJob.scheduleFitnessSyncJob(getApplicationContext(), triggerAtMillis);
        String msg = String.format("Scheduling another sync in %d millisec.", triggerAtMillis);
        UserLogging.logBgBleInfo(msg);
    }

    private String getCurrentPersonString() {
        return this.fitnessSync.getCurrentPerson().toString();
    }
}

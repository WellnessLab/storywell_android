package edu.neu.ccs.wellness.storytelling.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OneTimeWorkRequest.Builder;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.concurrent.futures.CallbackToFutureAdapter.Resolver;

import com.google.common.util.concurrent.ListenableFuture;

import edu.neu.ccs.wellness.people.Group;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

public class FitnessSyncWorker extends ListenableWorker
        implements FitnessSync.OnFitnessSyncProcessListener{
    private static final String TAG = "SWELL-SYNC-WORK";
    private static final int LONG_SYNC_TIMEOUT_MILLIS = 5 * 60 * 1000; // Five minutes

    private FitnessSync fitnessSync;
    private Group group;
    private Completer<Result> mCompleter;

    /* STATIC METHODS */
    static void placeSyncWork(Context context) {
        OneTimeWorkRequest fitnessSyncWorkRequest = new Builder(FitnessSyncWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(fitnessSyncWorkRequest);
    }

    /* CONSTRUCTOR */
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public FitnessSyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        Storywell storywell = new Storywell(getApplicationContext());
        group = storywell.getGroup();
        fitnessSync = new FitnessSync(getApplicationContext(), this);
        fitnessSync.setSyncTimeoutMillis(LONG_SYNC_TIMEOUT_MILLIS);
    }

    /* METHODS */
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(new Resolver<Result>() {
            @Nullable
            @Override
            public Object attachCompleter(@NonNull Completer<Result> completer) throws Exception {
                Log.d(TAG, "Starting FitnessSyncJobService");
                UserLogging.logStartBgBleSync();

                mCompleter = completer;
                fitnessSync.perform(group);

                return true;
            }
        });
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
                completeSync(true);
                break;
            case FAILED:
                Log.d(TAG, "Sync failed");
                UserLogging.logStopBgBleSync(false);
                completeSync(false);
                break;

        }
    }

    private void completeSync(boolean isSuccessful) {
        Log.d(TAG, "Stopping sync worker");
        this.fitnessSync.stop();
        if (isSuccessful) {
            mCompleter.set(Result.success());
        } else {
            mCompleter.set(Result.failure());
        }
    }

    private String getCurrentPersonString() {
        return this.fitnessSync.getCurrentPerson().toString();
    }
}

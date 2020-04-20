package edu.neu.ccs.wellness.storytelling.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.SplashScreenActivity;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

public class FitnessSyncJobService extends JobService
        implements FitnessSync.OnFitnessSyncProcessListener {

    private static final String TAG = "SWELL-SVC";
    public static final int LONG_SYNC_TIMEOUT_MILLIS = 3 * 60 * 1000; // Three minutes
    private FitnessSync fitnessSync;
    private JobParameters params;

    public FitnessSyncJobService() {
    }

    /* BASE METHODS */
    @Override
    public boolean onStartJob(JobParameters params) {
        Storywell storywell = new Storywell(getApplicationContext());

        this.params = params;

        if (storywell.userHasLoggedIn()) {
            Log.d(TAG, "Starting FitnessSyncJobService");
            UserLogging.logStartBgBleSync();

            startForeground(this);
            this.fitnessSync = new FitnessSync(getApplicationContext(), this);
            this.fitnessSync.setSyncTimeoutMillis(LONG_SYNC_TIMEOUT_MILLIS);
            this.fitnessSync.perform(storywell.getGroup());

            return true;
        } else {
            return false;
        }
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
        switch (syncStatus) {
            case NO_NEW_DATA:
                UserLogging.logStopBgBleSync(true);
                completeSync();
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
                String msg = "Done syncing: " + getCurrentPersonString();
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
                Log.d(TAG, "Sync failed");
                UserLogging.logStopBgBleSync(false);
                completeSync();
                break;

        }
    }

    private void completeSync() {
        Log.d(TAG, "Stopping sync service");
        this.fitnessSync.stop();
        if (params != null) {
            this.jobFinished(params, false);
        }
        this.stopForeground(true);
    }

    private String getCurrentPersonString() {
        if (this.fitnessSync.getCurrentPerson() != null) {
            return this.fitnessSync.getCurrentPerson().toString();
        } else {
            return "";
        }
    }

    /* FOREGROUND SERVICE */
    private static void startForeground(JobService jobService) {
        createNotificationChannel(jobService);

        Intent intent = new Intent(jobService, SplashScreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                jobService, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(jobService, TAG)
                .setContentTitle("Downloading data from your fitness bands.")
                .setSmallIcon(R.drawable.ic_sync_black_24dp)
                .setColor(jobService.getResources().getColor(R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();

        jobService.startForeground(1, notification);
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    TAG,
                    "Storywell Fitness Download",
                    NotificationManager.IMPORTANCE_LOW);

            context.getSystemService(NotificationManager.class)
                    .createNotificationChannel(serviceChannel);
        }
    }
}

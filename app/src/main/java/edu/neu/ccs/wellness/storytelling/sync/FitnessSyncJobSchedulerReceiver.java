package edu.neu.ccs.wellness.storytelling.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FitnessSyncJobSchedulerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FitnessSyncJob.placeFitnessSyncJob(context);
    }
}
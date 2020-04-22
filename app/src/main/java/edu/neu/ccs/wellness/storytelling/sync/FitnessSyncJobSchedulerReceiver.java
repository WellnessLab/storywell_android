package edu.neu.ccs.wellness.storytelling.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

public class FitnessSyncJobSchedulerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        UserLogging.logBgBleInfo("Updating settings before placing sync request");

        SynchronizedSettingRepository.updateLocalInstance(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                FitnessSyncJob.placeFitnessSyncJob(context);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                String msg = "Can't update setting before placing sync request";
                UserLogging.logBleError(msg);
            }
        }, context);
    }
}

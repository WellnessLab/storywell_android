package edu.neu.ccs.wellness.storytelling.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.notifications.BatteryReminderReceiver;
import edu.neu.ccs.wellness.storytelling.notifications.RegularReminderReceiver;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.storytelling.sync.FitnessSyncJob;

/**
 * Created by hermansaksono on 2/5/19.
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SWELL", "Starting BootReceiver");
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            scheduleAllJobs(context);
        }
        if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            scheduleAllJobs(context);
        }
    }

    private static void scheduleAllJobs(Context context) {
        Storywell storywell = new Storywell(context);
        if (storywell.userHasLoggedIn()) {
            FitnessSyncJob.scheduleRepeatingFitnessSyncJob(context);

            RegularReminderReceiver.scheduleRegularReminders(context);
            BatteryReminderReceiver.scheduleBatteryReminders(context);

            SynchronizedSetting setting = storywell.getSynchronizedSetting();
            setting.setRegularReminderSet(true);
            SynchronizedSettingRepository.saveLocalAndRemoteInstance(setting, context);
        }
    }

}

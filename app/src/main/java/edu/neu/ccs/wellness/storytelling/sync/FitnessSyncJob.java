package edu.neu.ccs.wellness.storytelling.sync;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.notifications.Constants;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.utils.date.HourMinute;

public class FitnessSyncJob {
    public static final int JOB_ID = 977;
    public static final int REQUEST_CODE = 977;
    public static final long INTERVAL = AlarmManager.INTERVAL_DAY;
    private static final String TAG = "SWELL-SVC";

    @TargetApi(23)
    public static void placeFitnessSyncJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, FitnessSyncJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent);
        builder.setMinimumLatency(10 * 1000); // wait at least
        builder.setOverrideDeadline(15 * 1000); // maximum delay
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
        Log.d(TAG, "FitnessSyncJob placed");
    }

    /**
     * Schedule a fitness sync in the given millisec.
     * @param context
     * @param triggerAtMillis
     */
    public static void scheduleFitnessSyncJob(Context context, int triggerAtMillis) {
        // Creates the PendingIntent
        PendingIntent syncIntent = getReminderReceiverIntent(context);

        // Set up the AlarmManager
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Try to cancel existing alarm
        alarmMgr.cancel(syncIntent);

        // Schedule the alarm
        long actualMillis = System.currentTimeMillis() + triggerAtMillis;
        alarmMgr.set(AlarmManager.RTC_WAKEUP, actualMillis, syncIntent);
        Log.d(TAG, String.format("FitnessSync scheduled in %d millis.", triggerAtMillis));
    }

    /**
     * Schedule a sync a few hours before the challenge end time (as specified
     * in the User's configuration)
     * @param context
     */
    public static void scheduleRepeatingFitnessSyncJob(Context context) {
        Storywell storywell = new Storywell(context);

        // Creates the PendingIntent
        PendingIntent syncIntent = getReminderReceiverIntent(context);

        // Determine the time for the alarm reminder
        SynchronizedSetting setting = storywell.getSynchronizedSetting();
        HourMinute hourMinute = setting.getChallengeEndTime();
        Calendar reminderCal = getReminderCalendar(hourMinute);
        long reminderMillis = reminderCal.getTimeInMillis();

        // Set up the AlarmManager
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Try to cancel existing alarm
        alarmMgr.cancel(syncIntent);

        // Schedule the alarm
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, reminderMillis, INTERVAL, syncIntent);

        Log.d(TAG, String.format("FitnessSync scheduled every %s.", reminderCal.toString()));

    }

    private static PendingIntent getReminderReceiverIntent(Context context) {
        return PendingIntent.getBroadcast(
                context, REQUEST_CODE, getAlarmIntent(context), 0);
    }

    private static Calendar getReminderCalendar(HourMinute hourMinute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, getReminderHour(hourMinute));
        calendar.set(Calendar.MINUTE, hourMinute.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static int getReminderHour(HourMinute hourMinute) {
        return Math.max(hourMinute.getHour() + Constants.BATTERY_REMINDER_OFFSET, 0);
    }

    private static Intent getAlarmIntent(Context context) {
        return new Intent(context, FitnessSyncJobSchedulerReceiver.class);
    }
}

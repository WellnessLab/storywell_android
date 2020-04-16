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
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;
import edu.neu.ccs.wellness.utils.WellnessDate;
import edu.neu.ccs.wellness.utils.date.HourMinute;

public class FitnessSyncJob {
    public static final int JOB_ID = 977;
    public static final int REQUEST_CODE = 977;
    public static final long INTERVAL_DAILY = AlarmManager.INTERVAL_DAY;
    public static final long INTERVAL_INTRADAY = 3 * AlarmManager.INTERVAL_HOUR;
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
        UserLogging.logBgBleSyncPlaced();
    }

    /**
     * Determines whether a repeating fitness sync job has been scheduled.
     * @param context
     * @return
     */
    public static boolean isRepeatingFitnessJobScheduled(Context context) {
        return null != PendingIntent.getBroadcast(
                context, REQUEST_CODE, getAlarmIntent(context), PendingIntent.FLAG_NO_CREATE);
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

        // Log the event
        String dateString = WellnessDate.getDateStringRFC(actualMillis);
        int triggerAtSeconds = triggerAtMillis / 1000;
        Log.d(TAG, String.format(
                "FitnessSync scheduled in %d seconds at %s.", triggerAtSeconds, dateString));
    }

    /**
     * Schedule a sync a few hours before the challenge end time (as specified
     * in the User's configuration)
     * @param context
     */
    public static void scheduleRepeatingFitnessSyncJob(Context context) {
        Storywell storywell = new Storywell(context);
        SynchronizedSetting setting = storywell.getSynchronizedSetting();

        // Schedule the alarms
        HourMinute[] hourMinutes = new HourMinute[2];
        long[] intervals = new long[2];

        HourMinute challengeEndTime = setting.getChallengeEndTime();
        challengeEndTime.setHour(challengeEndTime.getHour() + Constants.BATTERY_REMINDER_OFFSET );
        hourMinutes[0] = challengeEndTime;
        intervals[0] = INTERVAL_DAILY;

        hourMinutes[1] = new HourMinute();
        hourMinutes[1].setHour(7);
        hourMinutes[1].setMinute(0);
        intervals[1] = INTERVAL_INTRADAY;

        /*
        hourMinutes[2] = new HourMinute();
        hourMinutes[2].setHour(12);
        hourMinutes[2].setMinute(30);
        intervals[2] = INTERVAL_DAILY;

        hourMinutes[3] = new HourMinute();
        hourMinutes[3].setHour(22);
        hourMinutes[3].setMinute(0);
        intervals[3] = INTERVAL_DAILY;
        */

        scheduleSyncAt(hourMinutes, intervals, context);
    }

    private static void scheduleSyncAt(HourMinute[] times, long[] intervals, Context context) {

        // Creates the PendingIntent
        PendingIntent syncIntent = getReminderReceiverIntent(context);

        // Set up the AlarmManager
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Try to cancel existing alarm
        alarmMgr.cancel(syncIntent);

        // Schedule the alarm

        for (int i = 0; i < times.length; i++) {
            HourMinute hourMinute = times[i];
            long interval = intervals[i];

            Calendar reminderCal = getReminderCalendar(hourMinute);
            long timeMillis = reminderCal.getTimeInMillis();
            String dateString = WellnessDate.getDateStringRFC(timeMillis);
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, timeMillis, interval, syncIntent);
            Log.d(TAG, String.format("FitnessSync scheduled every %s.", dateString));
        }

    }

    private static PendingIntent getReminderReceiverIntent(Context context) {
        return PendingIntent.getBroadcast(
                context, REQUEST_CODE, getAlarmIntent(context), 0);
    }

    private static Calendar getReminderCalendar(HourMinute hourMinute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourMinute.getHour());
        calendar.set(Calendar.MINUTE, hourMinute.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static Intent getAlarmIntent(Context context) {
        return new Intent(context, FitnessSyncJobSchedulerReceiver.class);
    }
}

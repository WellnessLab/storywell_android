package edu.neu.ccs.wellness.storytelling.notifications;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import edu.neu.ccs.wellness.notifications.RegularNotificationManager;
import edu.neu.ccs.wellness.storytelling.HomeActivity;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.storytelling.sync.FitnessSyncJob;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

/**
 * Created by hermansaksono on 2/9/19.
 */

public class FcmNotificationService extends FirebaseMessagingService {
    public static final String KEY_TAG = "command";
    public static final String KEY_DATA_NOTIF_TITLE = "title";
    public static final String KEY_DATA_NOTIF_BODY = "body";
    public static final String CMD_BG_SYNC_NOW = "doBgSyncNow";
    public static final String KEY_HOME_TAB_TO_SHOW = "homeTabToShow";
    public static final String NOTIF_GEOSTORY_UPDATE = "addNotifUpdate";
    public static final String NOTIF_BG_SYNC_PACKAGE = "edu.neu.ccs.wellness.geostory.activity";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            doHandleMessage(remoteMessage.getNotification());
        }

        if (remoteMessage.getData() != null) {
            if (remoteMessage.getData().containsKey(KEY_TAG)) {
                doHandleFcmCommand(remoteMessage);
            }
        }
    }

    private void doHandleMessage(@NotNull RemoteMessage.Notification notification) {
        // Log.d("SWELL", "Receiving an FCM notification: " + notification);

        String channelId = notification.getChannelId();
        RegularNotificationManager notifMgr = new RegularNotificationManager(channelId);

        if (channelId == null) {
            return;
        }

        switch (channelId) {
            case "Announcements":
                // Disabled this because it's handled by the SDK: tryShowUpdates(notification);
                break;
            case "Updates":
                notifMgr.showNotification(
                        Constants.FCM_NOTIFICATION_ID,
                        notification.getTitle(), notification.getBody(),
                        Constants.DEFAULT_NOTIFICATION_ICON_RESID,
                        getRetrievingActivityIntent(getApplicationContext()),
                        getApplicationContext());
                break;
            default:
                notifMgr.showNotification(
                        Constants.FCM_NOTIFICATION_ID,
                        notification.getTitle(), notification.getBody(),
                        Constants.DEFAULT_NOTIFICATION_ICON_RESID,
                        getGeostoryHomeActivityIntent(getApplicationContext()),
                        getApplicationContext());
                break;
        }
    }

    private void doHandleFcmCommand(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String command = data.get(KEY_TAG);
        // Log.d("SWELL", String.format("Receiving an FCM command: %s.", command));
        switch (command) {
            case NOTIF_GEOSTORY_UPDATE:
                tryShowUpdateNotification(data);
                addUnreadGeostoryNotification();
                break;
            case CMD_BG_SYNC_NOW:
                FitnessSyncJob.scheduleFitnessSyncJob(getApplicationContext(), 1000);
                UserLogging.logBgBleSyncRequested();
                break;
            default:
                break;
        }
    }

    private void tryShowUpdateNotification(Map<String, String> data) {
        String channelId = getString(R.string.notification_updates_channel_id);
        String title = data.get(KEY_DATA_NOTIF_TITLE);
        String body = data.get(KEY_DATA_NOTIF_BODY);

        if (title != null && body != null) {
            RegularNotificationManager notifMgr = new RegularNotificationManager(channelId);
            notifMgr.showNotification(
                    Constants.FCM_STORY_UPDATE_NOTIFICATION_ID,
                    title, body,
                    Constants.DEFAULT_NOTIFICATION_ICON_RESID,
                    getGeostoryHomeActivityIntent(getApplicationContext()),
                    getApplicationContext());
        }
    }

    private void addUnreadGeostoryNotification() {
        SynchronizedSetting setting = new Storywell(this).getSynchronizedSetting();
        setting.getNotificationInfo().setNewGeoStoryExist(true);

        SynchronizedSettingRepository.saveLocalAndRemoteInstance(setting, this);

        Intent intent = new Intent();
        intent.setAction(NOTIF_BG_SYNC_PACKAGE);
        intent.putExtra(KEY_TAG, NOTIF_GEOSTORY_UPDATE);
        sendBroadcast(intent);
    }

    private static Intent getRetrievingActivityIntent(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    private static Intent getGeostoryHomeActivityIntent(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(HomeActivity.KEY_DEFAULT_TAB, HomeActivity.TAB_GEOSTORY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d("SWELL", "Refreshed token: " + token);

        if(SynchronizedSettingRepository.isAuth()) {
            saveFCMTokenToSynchronizedSetting(token, this);
        }
    }

    private static void saveFCMTokenToSynchronizedSetting(String token, Context context) {
        DatabaseReference ref = SynchronizedSettingRepository.getDefaultFirebaseRepository(context);
        ref.child(SynchronizedSetting.KEY_FCM_TOKEN).setValue(token);
    }

    /**
     * Retrieve FCM token and save it under {@link SynchronizedSetting}
     */
    public static void initializeFCM(Context context) {
        getFCMToken(context);
    }

    private static void getFCMToken(final Context context) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("Swell", "FCM getInstanceId failed", task.getException());
                            return;
                        } else {
                            String token = task.getResult().getToken();
                            saveFCMToken(token, context);
                        }
                    }
                });
    }

    private static void saveFCMToken(String token, Context context) {
        DatabaseReference ref = SynchronizedSettingRepository.getDefaultFirebaseRepository(context);
        ref.child(SynchronizedSetting.KEY_FCM_TOKEN).setValue(token);
    }
}

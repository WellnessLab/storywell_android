package edu.neu.ccs.wellness.storytelling.settings;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import edu.neu.ccs.wellness.storytelling.R;

public class AppSetting {
    private final FirebaseRemoteConfig mFirebaseRemoteConfig;

    public AppSetting() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        initialize(null, null);
        /*
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        */
    }

    public AppSetting(OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        initialize(successListener, failureListener);
    }

    public void initialize(final OnSuccessListener<Void> successListener,
                           final OnFailureListener failureListener) {
        Task<Void> task = mFirebaseRemoteConfig.fetch();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mFirebaseRemoteConfig.activateFetched();
                mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

                if (successListener != null) {
                    successListener.onSuccess(aVoid);
                }

                Log.i("SWELL", "AppSetting updated.");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (failureListener != null) {
                    failureListener.onFailure(e);
                }
                Log.i("SWELL", "Failed updating AppSetting.");
            }
        });
    }

    public String getWelcomeMessage() {
        return mFirebaseRemoteConfig.getString("welcome_message");
    }

    public int getFitnessSyncIntervalIntraday() {
        return (int) mFirebaseRemoteConfig.getLong("fitness_sync_intraday_interval");
    }
}

package edu.neu.ccs.wellness.storytelling.settings;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import edu.neu.ccs.wellness.storytelling.R;

public class AppSetting {
    private static final int MINIMUM_FETCH_INTERVAL_SECONDS = 3600;
    private final FirebaseRemoteConfig mFirebaseRemoteConfig;

    public AppSetting() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(MINIMUM_FETCH_INTERVAL_SECONDS)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
    }

    public int getFitnessSyncIntervalIntraday() {
        return (int) mFirebaseRemoteConfig.getLong("fitness_sync_intraday_interval");
    }
}

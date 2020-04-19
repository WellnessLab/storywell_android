package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.content.Context;
import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import edu.neu.ccs.wellness.geostory.FirebaseUserGeoStoryMetaRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryReaction;
import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryMapLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryReactionsLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.UserStoryMapMetaLiveData;

import edu.neu.ccs.wellness.geostory.FirebaseGeoStoryRepository;

public class GeoStoryMapViewModel extends AndroidViewModel {

    private GeoStoryMapLiveData geoStoryMapLiveData;
    private UserStoryMapMetaLiveData userMetaLiveData;
    private GeoStoryReactionsLiveData geoStoryReactionsLiveData;

    private final String groupName;

    public GeoStoryMapViewModel(Application application) {
        super(application);
        this.groupName = new Storywell(application.getApplicationContext()).getGroup().getName();
    }

    @NonNull
    public LiveData<Map<String, GeoStory>> getGeoStoryMapLiveData() {
        if (this.geoStoryMapLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.geoStoryMapLiveData = new GeoStoryMapLiveData(firebaseDbRef
                    .child(FirebaseGeoStoryRepository.FIREBASE_GEOSTORY_ROOT)
                    .orderByChild(GeoStory.KEY_LAST_UPDATE_TIMESTAMP), groupName);
        }
        return this.geoStoryMapLiveData;
    }

    @NonNull
    public LiveData<UserGeoStoryMeta> getUserStoryMetaLiveData(Context context) {
        if (this.userMetaLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.userMetaLiveData = new UserStoryMapMetaLiveData(firebaseDbRef
                    .child(FirebaseUserGeoStoryMetaRepository.FIREBASE_ROOT)
                    .child(groupName));
        }
        return this.userMetaLiveData;
    }

    @NonNull
    public LiveData<Map<String, GeoStoryReaction>> getGeoStoryReactionsLiveData(String geoStoryId) {
        if (this.geoStoryReactionsLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.geoStoryReactionsLiveData = new GeoStoryReactionsLiveData(firebaseDbRef
                    .child(FirebaseGeoStoryRepository.FIREBASE_REACTIONS_ROOT)
                    .child(geoStoryId));
        }
        return this.geoStoryReactionsLiveData;
    }
}

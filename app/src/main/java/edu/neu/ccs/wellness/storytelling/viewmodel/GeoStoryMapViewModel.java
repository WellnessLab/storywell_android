package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import edu.neu.ccs.wellness.geostory.FirebaseUserGeoStoryMetaRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryMapLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.UserStoryMapMetaLiveData;

import edu.neu.ccs.wellness.geostory.FirebaseGeoStoryRepository;

public class GeoStoryMapViewModel extends AndroidViewModel {

    private GeoStoryMapLiveData geoStoryMapLiveData;
    private UserStoryMapMetaLiveData userMetaLiveData;

    public GeoStoryMapViewModel(Application application) {
        super(application);
    }

    @NonNull
    public LiveData<Map<String, GeoStory>> getGeoStoryMapLiveData() {
        if (this.geoStoryMapLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.geoStoryMapLiveData = new GeoStoryMapLiveData(firebaseDbRef
                    .child(FirebaseGeoStoryRepository.FIREBASE_GEOSTORY_ROOT)
                    .orderByChild(GeoStory.KEY_LAST_UPDATE_TIMESTAMP));
        }
        return this.geoStoryMapLiveData;
    }

    @NonNull
    public LiveData<UserGeoStoryMeta> getUserStoryMetaLiveData(Context context) {
        if (this.userMetaLiveData == null) {
            String groupName = new Storywell(context).getGroup().getName();
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.userMetaLiveData = new UserStoryMapMetaLiveData(firebaseDbRef
                    .child(FirebaseUserGeoStoryMetaRepository.FIREBASE_ROOT)
                    .child(groupName));
        }
        return this.userMetaLiveData;
    }
}

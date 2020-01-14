package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import edu.neu.ccs.wellness.storymap.GeoStory;
import edu.neu.ccs.wellness.storymap.UserGeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.UserStoryMapMetaLiveData;

import static edu.neu.ccs.wellness.storymap.FirebaseGeoStoryRepository.FIREBASE_GEOSTORY_ROOT;
import static edu.neu.ccs.wellness.storymap.FirebaseGeoStoryRepository.FIREBASE_GEOSTORY_META_ROOT;

public class StoryMapViewModel extends AndroidViewModel {

    private StoryMapLiveData storyMapLiveData;
    private UserStoryMapMetaLiveData userMetaLiveData;

    public StoryMapViewModel(Application application) {
        super(application);
    }

    @NonNull
    public LiveData<Map<String, GeoStory>> getStoryMapLiveData() {
        if (this.storyMapLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.storyMapLiveData = new StoryMapLiveData(firebaseDbRef
                    .child(FIREBASE_GEOSTORY_ROOT)
                    .orderByChild(GeoStory.KEY_LAST_UPDATE_TIMESTAMP));
        }
        return this.storyMapLiveData;
    }

    @NonNull
    public LiveData<UserGeoStoryMeta> getUserStoryMetaLiveData(Context context) {
        if (this.userMetaLiveData == null) {
            String groupName = new Storywell(context).getGroup().getName();
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.userMetaLiveData = new UserStoryMapMetaLiveData(firebaseDbRef
                    .child(FIREBASE_GEOSTORY_META_ROOT)
                    .child(groupName));
        }
        return this.userMetaLiveData;
    }
}

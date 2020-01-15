package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import edu.neu.ccs.wellness.storymap.GeoStory;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.homeview.UserGeoStoryLiveData;

import static edu.neu.ccs.wellness.storymap.FirebaseGeoStoryRepository.FIREBASE_GROUP_GEOSTORY_ROOT;

public class UserGeoStoryViewModel extends AndroidViewModel {

    private UserGeoStoryLiveData userGeoStoryLiveData;

    public UserGeoStoryViewModel(Application application) {
        super(application);
    }

    @NonNull
    public LiveData<Map<String, GeoStory>> getStoryMapLiveData(String promptId) {
        Storywell storywell = new Storywell(getApplication());
        String userId = storywell.getUser().getUsername();

        if (this.userGeoStoryLiveData == null) {
            DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
            this.userGeoStoryLiveData = new UserGeoStoryLiveData(firebaseDbRef
                    .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                    .child(userId)
                    .child(promptId));
        }
        return this.userGeoStoryLiveData;
    }
}

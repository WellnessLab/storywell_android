package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;

public class UserStoryMapMetaLiveData extends LiveData<UserGeoStoryMeta> {
    private final Query query;
    private final StoryMapMetaEventListener listener = new StoryMapMetaEventListener();

    /* CONSTRUCTOR */
    public UserStoryMapMetaLiveData(Query ref) {
        this.query = ref;
    }

    /* SUPERCLASS METHODS */
    @Override
    protected void onActive() {
        query.addValueEventListener(listener);
    }

    @Override
    protected void onInactive() {
        query.removeEventListener(listener);
    }

    /* EVENT LISTENER CLASS */
    private class StoryMapMetaEventListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()) {
                UserGeoStoryMeta userMeta = dataSnapshot.getValue(UserGeoStoryMeta.class);
                userMeta.setUnreadStories(getUnreadStories(dataSnapshot));
                setValue(userMeta);
            } else {
                setValue(new UserGeoStoryMeta());
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }

    private static Map<String, String> getUnreadStories(DataSnapshot dataSnapshot) {
        Map<String, String> unreadStories = new HashMap<>();
        DataSnapshot unreadStoriesDS = dataSnapshot.child(UserGeoStoryMeta.KEY_READ_STORIES);
        if (unreadStoriesDS.exists()) {
            for (DataSnapshot entry : unreadStoriesDS.getChildren()) {
                unreadStories.put(entry.getKey(), entry.getValue(String.class));
            }
        }
        return unreadStories;
    }
}



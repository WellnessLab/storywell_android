package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import edu.neu.ccs.wellness.storymap.UserGeoStoryMeta;

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
                setValue(dataSnapshot.getValue(UserGeoStoryMeta.class));
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }
}



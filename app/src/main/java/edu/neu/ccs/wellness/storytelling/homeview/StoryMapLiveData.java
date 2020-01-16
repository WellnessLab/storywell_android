package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import edu.neu.ccs.wellness.geostory.GeoStory;

public class StoryMapLiveData extends LiveData<Map<String, GeoStory>> {
    private final Query query;
    private final StoryMapEventListener listener = new StoryMapEventListener();

    /* CONSTRUCTOR */
    public StoryMapLiveData(Query ref) {
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
    private class StoryMapEventListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()) {
                setValue(getGeoStoryHashMap(dataSnapshot));
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }

    private static Map<String, GeoStory> getGeoStoryHashMap(DataSnapshot dataSnapshot) {
        Map<String, GeoStory> geoStoryMap = new ArrayMap<>();

        for (DataSnapshot entry : dataSnapshot.getChildren()) {
            geoStoryMap.put(entry.getKey(), entry.getValue(GeoStory.class));
        }

        return geoStoryMap;
    }
}


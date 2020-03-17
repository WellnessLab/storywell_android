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

public class GeoStoryMapLiveData extends LiveData<Map<String, GeoStory>> {
    private final Query query;
    private final StoryMapEventListener listener = new StoryMapEventListener();
    private int minSteps = Integer.MAX_VALUE;
    private int maxSteps = Integer.MIN_VALUE;

    /* CONSTRUCTOR */
    public GeoStoryMapLiveData(Query ref) {
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
            Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot entry : dataSnapshot.getChildren()) {
                    GeoStory geoStory = entry.getValue(GeoStory.class);
                    geoStoryMap.put(entry.getKey(), geoStory);

                    minSteps = Math.min(geoStory.getSteps(), minSteps);
                    maxSteps = Math.max(geoStory.getSteps(), maxSteps);
                }
            }
            setValue(geoStoryMap);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }

    /* GETTER METHODS */
    public int getMinSteps() {
        return minSteps;
    }

    public int getMaxSteps() {
        return maxSteps;
    }
}


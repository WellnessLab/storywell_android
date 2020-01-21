package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import edu.neu.ccs.wellness.geostory.GeoStory;

public class UserGeoStoryLiveData extends LiveData<Map<String, GeoStory>> {
    private final Query query;
    private final GeoStoryEventListener listener = new GeoStoryEventListener();

    /* CONSTRUCTOR */
    public UserGeoStoryLiveData(Query ref) {
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
    private class GeoStoryEventListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Map<String,GeoStory> geoStoryMap =  new HashMap<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    GeoStory geoStory = ds.getValue(GeoStory.class);
                    geoStoryMap.put(geoStory.getMeta().getPromptId(), geoStory);
                }
            }
            setValue(geoStoryMap);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }


}



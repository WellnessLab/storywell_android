package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import edu.neu.ccs.wellness.geostory.GeoStoryReaction;

public class GeoStoryReactionsLiveData extends LiveData<Map<String, GeoStoryReaction>> {
    private final Query query;
    private final GeoStoryReactionEventListener listener = new GeoStoryReactionEventListener();

    /* CONSTRUCTOR */
    public GeoStoryReactionsLiveData(Query ref) {
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
    private class GeoStoryReactionEventListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            Map<String, GeoStoryReaction> geoStoryMap = new ArrayMap<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot entry : dataSnapshot.getChildren()) {
                    GeoStoryReaction reaction = entry.getValue(GeoStoryReaction.class);
                    geoStoryMap.put(entry.getKey(), reaction);
                }
            }
            setValue(geoStoryMap);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }
}


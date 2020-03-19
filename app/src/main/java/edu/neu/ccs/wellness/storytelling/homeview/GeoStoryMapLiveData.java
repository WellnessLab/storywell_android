package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.neu.ccs.wellness.geostory.FirebaseGeoStoryRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;

public class GeoStoryMapLiveData extends LiveData<Map<String, GeoStory>> {
    private final Query query;
    private final StoryMapEventListener listener = new StoryMapEventListener();

    private final String groupName;
    Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
    private Set<String> userReactionsSet = new HashSet<>();
    private int minSteps = Integer.MAX_VALUE;
    private int maxSteps = Integer.MIN_VALUE;

    /* CONSTRUCTOR */
    public GeoStoryMapLiveData(Query ref, String groupName) {
        this.query = ref;
        this.groupName = groupName;
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
                geoStoryMap.clear();
                for (DataSnapshot entry : dataSnapshot.getChildren()) {
                    GeoStory geoStory = entry.getValue(GeoStory.class);
                    geoStoryMap.put(entry.getKey(), geoStory);

                    minSteps = Math.min(geoStory.getSteps(), minSteps);
                    maxSteps = Math.max(geoStory.getSteps(), maxSteps);
                }
                getReactions();
            }
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

    public Set<String> getUserReactionsSet() {
        return userReactionsSet;
    }

    /* DATA RETRIEVAL METHODS */
    private void getReactions() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(FirebaseGeoStoryRepository.FIREBASE_GROUP_GEOSTORY_REACTIONS_ROOT)
                .child(groupName);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userReactionsSet = getUserReactionSet(dataSnapshot);
                }

                setValue(geoStoryMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                setValue(geoStoryMap);
            }
        });
    }

    private Set<String> getUserReactionSet(DataSnapshot dataSnapshot) {
        Set<String> userReactionSet = new HashSet<>();

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            userReactionSet.add(ds.getKey());
        }

        return userReactionSet;
    }
}


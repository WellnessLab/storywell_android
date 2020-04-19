package edu.neu.ccs.wellness.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import edu.neu.ccs.wellness.story.interfaces.StorytellingContentManager;

public class StoryContentManager implements StorytellingContentManager {


    public static final String FIREBASE_PATH_CONTENT_STATE = "group_story_content_state";

    private String storyId;
    private Map<Integer, String> contentStateMap = new HashMap<>();
    private DatabaseReference firebaseDbRef;

    /**
     * Constructor
     * @param storyId
     * @param userId
     */
    public StoryContentManager(String storyId, String userId) {
        this.storyId = storyId;
        this.firebaseDbRef = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_PATH_CONTENT_STATE)
                .child(userId)
                .child(storyId);

        this.firebaseDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contentStateMap = getContentStateMap(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Converts a DataSnapshot to a StoryContentStateMap
     */
    private Map<Integer, String> getContentStateMap(@NonNull DataSnapshot dataSnapshot) {
        Map<Integer, String> contentStateMapOutput = new HashMap<>();

        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            int mapKey = Integer.valueOf(ds.getKey());
            String state = ds.getValue(String.class);
            contentStateMapOutput.put(mapKey, state);
        }
        return contentStateMapOutput;
    }

    /**
     * @return Returns the story id being used in this manager.
     */
    @Override
    public String getStoryId() {
        return this.storyId;
    }

    @Override
    public void setStatus(final int contentId, final String status) {
        this.firebaseDbRef
                .child(String.valueOf(contentId))
                .setValue(status, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError,
                                   @NonNull DatabaseReference databaseReference) {
                contentStateMap.put(contentId, status);
            }
        });
    }

    @Override
    public String getStatus(int contentId) {
        return contentStateMap.get(contentId);
    }
}

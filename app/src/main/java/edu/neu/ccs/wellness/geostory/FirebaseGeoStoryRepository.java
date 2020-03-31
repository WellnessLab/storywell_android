package edu.neu.ccs.wellness.geostory;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hermansaksono on 1/14/2020.
 */

public class FirebaseGeoStoryRepository {

    public static final String FIREBASE_GEOSTORY_ROOT = "all_geostory";
    public static final String FIREBASE_REACTIONS_ROOT = "all_geostory_reactions";
    public static final String FIREBASE_GROUP_GEOSTORY_ROOT = "group_geostory";
    public static final String FIREBASE_GROUP_GEOSTORY_REACTIONS_ROOT = "group_geostory_reactions";
    // public static final String FIREBASE_GEOSTORY_META_ROOT = "group_geostory_meta";
    private static final String GEOSTORY_YEAR_MONTH ="yyyy-MM";
    private static final int ONE_REACTION = 1;
    private static final int MINUS_ONE_REACTION = -1;
    private final String groupName;

    private DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
    private StorageReference firebaseStorageRef = FirebaseStorage.getInstance().getReference();
    private Map<String, GeoStory> userGeoStoryMap = new HashMap<>();

    private boolean isUploadQueueEmpty = true;

    /* CONSTRUCTOR */
    public FirebaseGeoStoryRepository(String groupName, String promptParentId) {
        this.getUserGeoStoriesFromFirebase(groupName, promptParentId);
        this.groupName = groupName;
    }

    /* METHODS */
    public boolean isReflectionResponded(String promptId) {
        return this.userGeoStoryMap.containsKey(promptId);
    }

    public String getRecordingURL(String promptId) {
        return this.userGeoStoryMap.get(promptId).getStoryUri();
    }

    public void putRecordingURL(GeoStory geoStory) {
        this.userGeoStoryMap.put(geoStory.getMeta().getPromptId(), geoStory);
    }

    public GeoStory getSavedGeoStory(String promptId) {
        return this.userGeoStoryMap.get(promptId);
    }

    public boolean isUploadQueued() {
        return this.isUploadQueueEmpty;
    }

    /* UPDATING REFLECTION URLS METHOD */
    public void getUserGeoStoriesFromFirebase(String groupName, String promptParentId) {
        this.firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(groupName)
                .child(promptParentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userGeoStoryMap = processGeoStories(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        userGeoStoryMap.clear();
                    }
                });
    }

    private static Map<String, GeoStory> processGeoStories(DataSnapshot dataSnapshot) {
        Map<String, GeoStory> geoStoryHashMap = new HashMap<>();
        if (dataSnapshot.exists()) {
            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                GeoStory geoStory =  ds.getValue(GeoStory.class);
                if (geoStory != null) {
                    String promptId = geoStory.getMeta().getPromptId();
                    geoStoryHashMap.put(promptId, geoStory);
                }
            }
        }
        return geoStoryHashMap;
    }

    /* GEOSTORY UPLOADING METHODS */
    /**
     * Upload the given {@param geoStory} as a recording file to Firebase Storage. Then put the
     * {@param geoStory} into Firebase DB as a global and also user-level {@link GeoStory}.
     * @param geoStory
     * @param path
     * @param listener
     */
    public void uploadGeoStoryFileToFirebase(final GeoStory geoStory, String path,
                                             final OnSuccessListener<GeoStory> listener) {
        final File localAudioFile = new File(path);
        final Uri audioUri = Uri.fromFile(localAudioFile);
        this.firebaseStorageRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(geoStory.getUsername())
                .child(geoStory.getMeta().getPromptParentId())
                .child(geoStory.getFilename())
                .putFile(audioUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        deleteLocalStoryFile(localAudioFile);
                        isUploadQueueEmpty = false;
                        addGeoStoryToFirebase(taskSnapshot, geoStory, listener);
                        // listener.onSuccess(taskSnapshot);
                    }
                });
    }

    private void deleteLocalStoryFile(File file) {
        try {
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Given the {@param taskSnapshot} upload, add the {@param geoStory} to Firebase as global
     * {@link GeoStory} as well as user-level {@link GeoStory}.
     * @param taskSnapshot
     * @param geoStory
     *
     */
    private void addGeoStoryToFirebase(UploadTask.TaskSnapshot taskSnapshot,
                                       final GeoStory geoStory,
                                       final OnSuccessListener<GeoStory> listener) {
        Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
        StorageMetadata metadata = taskSnapshot.getMetadata();
        final String gsUri = "gs://" + metadata.getBucket() + "/" + metadata.getPath();
        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                addGeoStory(geoStory, uri.toString(), gsUri, listener);
            }
        });
    }

    /**
     * Add the {@param geoStory} as a global {@link GeoStory}. Then add a user-level
     * {@link GeoStory}.
     * @param geoStory
     * @param audioUrl
     * @param gsUri
     */
    private void addGeoStory(GeoStory geoStory, String audioUrl, String gsUri,
                             final OnSuccessListener<GeoStory> listener) {
        // Put the reflection URI to Firebase DB
        DatabaseReference dbRef = this.firebaseDbRef
                .child(FIREBASE_GEOSTORY_ROOT)
                .push();

        geoStory.setStoryId(dbRef.getKey());
        geoStory.setStoryUri(audioUrl);
        geoStory.setGsUri(gsUri);

        dbRef.setValue(geoStory);

        listener.onSuccess(geoStory);

        this.addGroupGeoStory(geoStory);
    }

    /**
     * Add the {@param geoStory} as user-level {@link GeoStory}.
     * @param geoStory
     */
    private void addGroupGeoStory(GeoStory geoStory) {

        // Put the reflection URI to Firebase DB
        DatabaseReference dbRef = this.firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(geoStory.getUsername())
                .child(geoStory.getMeta().getPromptParentId())
                .child(geoStory.getStoryId());
        dbRef.setValue(geoStory);

        // Put reflection uri to the instance's field
        this.userGeoStoryMap.put(geoStory.getMeta().getPromptId(), geoStory);
    }

    /* REACTION METHODS */
    /**
     * Add reaction from the given user.
     * @param reactionerUserId
     * @param reactionUserName
     * @param geoStoryId
     * @param reactionType
     */
    public void addReaction(final String reactionerUserId, String reactionUserName,
                            final String geoStoryId, final int reactionType,
                            String geoStoryAuthor, int totalReactions) {
        final DatabaseReference reactionRef = firebaseDbRef
                .child(FIREBASE_REACTIONS_ROOT)
                .child(geoStoryId)
                .child(reactionerUserId);
        final GeoStoryReaction geoStoryReaction = new GeoStoryReaction(
                reactionerUserId, reactionUserName, geoStoryId, reactionType,
                totalReactions, geoStoryAuthor);

        reactionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    reactionRef.removeValue();
                    removeGroupReaction(reactionerUserId, geoStoryId);
                    addNumOfReactions(geoStoryId, MINUS_ONE_REACTION);
                } else {
                    reactionRef.setValue(geoStoryReaction);
                    addGroupReaction(reactionerUserId, geoStoryId, reactionType);
                    addNumOfReactions(geoStoryId, ONE_REACTION);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Do nothing
            }
        });
    }

    private void addGroupReaction(
            String reactionerUserId, final String geoStoryId, final int reactionType) {
        final DatabaseReference reactionRef = firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_REACTIONS_ROOT)
                .child(reactionerUserId)
                .child(geoStoryId);

        reactionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    reactionRef.removeValue();
                } else {
                    reactionRef.setValue(reactionType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Do nothing
            }
        });

    }

    private void removeGroupReaction(String reactionerUserId, final String geoStoryId) {
        final DatabaseReference reactionRef = firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_REACTIONS_ROOT)
                .child(reactionerUserId)
                .child(geoStoryId);

        reactionRef.removeValue();

    }

    /**
     * Remove reaction
     */
    public void removeReaction(final String reactionerUserId, final String geoStoryId) {
        final DatabaseReference reactionRef = firebaseDbRef
                .child(FIREBASE_REACTIONS_ROOT).child(geoStoryId).child(reactionerUserId);

        reactionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    reactionRef.removeValue();
                    removeGroupReaction(reactionerUserId, geoStoryId);
                    addNumOfReactions(geoStoryId, MINUS_ONE_REACTION);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Do nothing
            }
        });
    }


    /**
     * Increment the {@param geoStoryId} by {@param incrementBy}.
     * @param geostoryId
     * @param incrementBy
     */
    private void addNumOfReactions(String geostoryId, final int incrementBy) {
        DatabaseReference dbRef = this.firebaseDbRef
                .child(FIREBASE_GEOSTORY_ROOT)
                .child(geostoryId);

        dbRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                GeoStory geoStory = mutableData.getValue(GeoStory.class);

                if (geoStory == null) {
                    // Do nothing
                } else {
                    int numReactions = Math.max(
                            geoStory.getMeta().getNumReactions() + incrementBy, 0);
                    geoStory.getMeta().setNumReactions(numReactions);
                    mutableData.setValue(geoStory);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b,
                                   @Nullable DataSnapshot dataSnapshot) {
                // Do nothing. Transaction completed
            }
        });
    }

    public static void getReactionsFromAGeoStory(String geoStoryId, ValueEventListener listener) {
        final DatabaseReference reactionRef = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_REACTIONS_ROOT)
                .child(geoStoryId);

        reactionRef.addListenerForSingleValueEvent(listener);
    }

}

package edu.neu.ccs.wellness.storymap;

import android.net.Uri;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by hermansaksono on 1/14/2020.
 */

public class FirebaseGeoStoryRepository {

    public static final String FIREBASE_GEOSTORY_ROOT = "all_geostory";
    public static final String FIREBASE_GROUP_GEOSTORY_ROOT = "group_geostory";
    public static final String FIREBASE_GEOSTORY_META_ROOT = "group_geostory_meta";

    private static final String FIRESTORE_FILENAME_FORMAT = "geostory_group_%s_parent_%s_%s.3gp";
    private static final String GEOSTORY_DATE_FORMAT ="yyyy-MM-dd HH:mm:ss";
    private static final String GEOSTORY_YEAR_MONTH ="yyyy-MM";

    private DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
    private StorageReference firebaseStorageRef = FirebaseStorage.getInstance().getReference();
    private Map<String, GeoStory> userGeoStoryMap = new HashMap<>();

    private boolean isUploadQueueEmpty = true;

    /* CONSTRUCTOR */
    public FirebaseGeoStoryRepository(String groupName, String storyId) {
        this.getUserGeoStoriesFromFirebase(groupName, storyId);
    }

    /* METHODS */
    public boolean isReflectionResponded(String storyId) {
        return this.userGeoStoryMap.containsKey(storyId);
    }

    public GeoStory getRecordingURL(String storyId) {
        return this.userGeoStoryMap.get(storyId);
    }

    public void putRecordingURL(String storyId, GeoStory geoStory) {
        this.userGeoStoryMap.put(storyId, geoStory);
    }

    public boolean isUploadQueued() {
        return this.isUploadQueueEmpty;
    }

    /* UPDATING REFLECTION URLS METHOD */
    public void getUserGeoStoriesFromFirebase(String groupName, String storyId) {
        this.firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(groupName)
                .child(storyId)
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
                geoStoryHashMap.put(ds.getKey(), ds.getValue(GeoStory.class));
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
                                             final OnSuccessListener<UploadTask.TaskSnapshot>
                                                     listener) {
        final Calendar recordingCal = (Calendar.getInstance(Locale.US));
        String firebaseName = getStoryFilename(geoStory, recordingCal);
        final File localAudioFile = new File(path);
        final Uri audioUri = Uri.fromFile(localAudioFile);
        this.firebaseStorageRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(geoStory.getUsername())
                .child(geoStory.getMeta().getPromptId())
                .child(firebaseName)
                .putFile(audioUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        deleteLocalStoryFile(localAudioFile);
                        isUploadQueueEmpty = false;
                        addGeoStoryToFirebase(taskSnapshot, geoStory, recordingCal);
                        listener.onSuccess(taskSnapshot);
                    }
                });
    }

    private static String getStoryFilename(GeoStory geoStory, Calendar cal) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(GEOSTORY_DATE_FORMAT, Locale.US);
        String dateString = dateFormatter.format(cal.getTime());
        return String.format(FIRESTORE_FILENAME_FORMAT, geoStory.getUsername(),
                geoStory.getMeta().getPromptId(), dateString);
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
     * @param recordingCal
     */
    private void addGeoStoryToFirebase(UploadTask.TaskSnapshot taskSnapshot,
                                       final GeoStory geoStory, final Calendar recordingCal) {
        Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                addGeoStory(geoStory, uri.toString(), recordingCal);
            }
        });
    }

    /**
     * Add the {@param geoStory} as a global {@link GeoStory}. Then add a user-level
     * {@link GeoStory}.
     * @param geoStory
     * @param audioUrl
     * @param recordingCal
     */
    private void addGeoStory(GeoStory geoStory, String audioUrl, Calendar recordingCal) {

        // Put the reflection URI to Firebase DB
        DatabaseReference dbRef = this.firebaseDbRef
                .child(FIREBASE_GEOSTORY_ROOT)
                .push();

        geoStory.setStoryId(dbRef.getKey());
        geoStory.setStoryUri(audioUrl);
        geoStory.setLastUpdateTimestamp(recordingCal.getTimeInMillis());

        dbRef.setValue(geoStory);

        this.addGroupGeoStory(geoStory);
    }

    /**
     * Add the {@param geoStory} as user-level {@link GeoStory}.
     * @param geoStory
     */
    private void addGroupGeoStory(GeoStory geoStory) {

        // Put the reflection URI to Firebase DB
        this.firebaseDbRef
                .child(FIREBASE_GROUP_GEOSTORY_ROOT)
                .child(geoStory.getMeta().getPromptId())
                .child(geoStory.getMeta().getPromptSubId())
                .setValue(geoStory);

        // Put reflection uri to the instance's field
        this.userGeoStoryMap.put(geoStory.getStoryId(), geoStory);
    }



}

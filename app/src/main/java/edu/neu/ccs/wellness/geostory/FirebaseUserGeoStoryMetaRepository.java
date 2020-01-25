package edu.neu.ccs.wellness.geostory;

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseUserGeoStoryMetaRepository {

    /* CONSTANTS */
    public static final String FIREBASE_ROOT = "group_geostory_meta";

    /* PRIVATE FIELDS */
    private DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();
    private final String userName;

    /* CONSTRUCTOR */
    public FirebaseUserGeoStoryMetaRepository(String userName) {
        this.userName = userName;
    }

    // SAVING UserGeoStoryMeta
    public void addStoryAsRead(final String geoStoryId) {
        final DatabaseReference dbRef = this.firebaseDbRef
                .child(FIREBASE_ROOT)
                .child(this.userName)
                .child(UserGeoStoryMeta.KEY_READ_STORIES)
                .child(geoStoryId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    dbRef.setValue(geoStoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}

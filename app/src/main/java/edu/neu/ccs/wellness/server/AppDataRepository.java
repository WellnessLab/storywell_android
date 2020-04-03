package edu.neu.ccs.wellness.server;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AppDataRepository {
    public static final String APP_DATA_PATH = "app_data";
    private DatabaseReference firebaseDbRef = FirebaseDatabase.getInstance().getReference();

    public void getAppDataValue(String path, ValueEventListener listener) {
        firebaseDbRef.child(APP_DATA_PATH).child(path).addListenerForSingleValueEvent(listener);
    }
}

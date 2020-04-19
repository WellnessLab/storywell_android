package edu.neu.ccs.wellness.storytelling.storyview;

import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * Created by hermansaksono on 1/22/19.
 */

public class CompletedChallengesLiveData extends LiveData<List<String>> {
    private final Query query;
    private final CompletedChallengeListListener listener = new CompletedChallengeListListener();

    /* CONSTRUCTOR */
    public CompletedChallengesLiveData(DatabaseReference ref) {
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
    @SuppressWarnings("unchecked")
    private class CompletedChallengeListListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            setValue((List<String>) dataSnapshot.getValue());
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
        }
    }
}

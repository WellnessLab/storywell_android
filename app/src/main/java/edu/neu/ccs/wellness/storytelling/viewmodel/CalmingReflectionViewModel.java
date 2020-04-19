package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import edu.neu.ccs.wellness.story.CalmingReflectionSet;
import edu.neu.ccs.wellness.story.CalmingReflectionRepository;

/**
 * Created by hermansaksono on 3/1/19.
 */

public class CalmingReflectionViewModel extends AndroidViewModel {

    private MutableLiveData<CalmingReflectionSet> calmingReflectionSetLiveData;
    private boolean isExists = false;

    public CalmingReflectionViewModel(Application application) {
        super(application);
    }

    /**
     * Get a {@link LiveData} that contains the requested {@link CalmingReflectionSet}.
     * @param reflGroupId
     * @return
     */
    public LiveData<CalmingReflectionSet> getCalmingReflectionGroup(String reflGroupId) {
        if (this.calmingReflectionSetLiveData == null) {
            this.calmingReflectionSetLiveData = new MutableLiveData<>();
            this.fetchReflectionGroup(CalmingReflectionSet.DEFAULT_ID);
        }
        return this.calmingReflectionSetLiveData;
    }

    private void fetchReflectionGroup(String calmingReflectionSetId) {
        CalmingReflectionRepository.getDatabaseReference(calmingReflectionSetId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                calmingReflectionSetLiveData.setValue(
                        CalmingReflectionRepository.getCalmingReflectionSet(dataSnapshot)
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}

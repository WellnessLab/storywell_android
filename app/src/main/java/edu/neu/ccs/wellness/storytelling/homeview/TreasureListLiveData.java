package edu.neu.ccs.wellness.storytelling.homeview;

import android.arch.lifecycle.LiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import edu.neu.ccs.wellness.reflection.ResponsePile;
import edu.neu.ccs.wellness.reflection.ResponsePileListFactory;

/**
 * Created by hermansaksono on 1/17/19.
 */

public class TreasureListLiveData extends LiveData<List<ResponsePile>> {
    private final Query query;
    private final ResponsePileListEventListener listener = new ResponsePileListEventListener();

    /* CONSTRUCTOR */
    public TreasureListLiveData(DatabaseReference ref) {
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
    private class ResponsePileListEventListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            setValue(ResponsePileListFactory.newInstance(dataSnapshot));
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    }

}

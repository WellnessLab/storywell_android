package edu.neu.ccs.wellness.storytelling.viewmodel.unused;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.AsyncTask;

import edu.neu.ccs.wellness.people.Group;
import edu.neu.ccs.wellness.server.RestServer;
import edu.neu.ccs.wellness.storytelling.Storywell;

/**
 * Created by hermansaksono on 5/16/18.
 */

public class GroupViewModel extends AndroidViewModel {

    private MutableLiveData<Group> mutableGroup = null;

    /* CONSTRUCTOR */
    public GroupViewModel(Application application) {
        super(application);
    }

    /* PUBLIC METHODS */
    public LiveData<Group> getUnitChallenge() {
        if (this.mutableGroup == null) {
            this.mutableGroup = new MutableLiveData<Group>();
            loadGroup();
        }
        return this.mutableGroup;
    }

    /* PRIVATE METHODS */
    private void loadGroup() {
        new LoadGroupAsync().execute();
    }

    /* ASYNCTASKS */
    private class LoadGroupAsync extends AsyncTask<Void, Integer, RestServer.ResponseType> {
        Storywell storywell = new Storywell(getApplication());
;
        protected RestServer.ResponseType doInBackground(Void... voids) {
            if (storywell.isServerOnline()) {
                mutableGroup.setValue(storywell.getGroup());
                return RestServer.ResponseType.SUCCESS_202;
            } else {
                return RestServer.ResponseType.NO_INTERNET;
            }
        }
    }
}

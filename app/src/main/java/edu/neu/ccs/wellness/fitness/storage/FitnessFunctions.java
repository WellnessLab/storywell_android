package edu.neu.ccs.wellness.fitness.storage;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

import edu.neu.ccs.wellness.storytelling.utils.StorywellPerson;

public class FitnessFunctions {

    private static final String FN_COMPUTE_DAILY_STEPS = "fitness-httpComputeDailySteps";
    private static final String KEY_PERSON_ID = "personId";

    private FirebaseFunctions mFunctions;

    /**
     * Constructor
     */
    public FitnessFunctions() {
        mFunctions = FirebaseFunctions.getInstance();
    }

    /**
     * Recalculate daily steps of a person (given the personId) on the particular day (given the
     * dateString) using the intra day fitness data.
     * @param person
     * @return
     */
    public Task<String> computeDailySteps(StorywellPerson person) {
        int personId = person.getPerson().getId();

        Map<String, Object> data = new HashMap<>();
        data.put(KEY_PERSON_ID, personId);

        return mFunctions
                .getHttpsCallable(FN_COMPUTE_DAILY_STEPS)
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }
}

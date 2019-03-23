package edu.neu.ccs.wellness.storytelling.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.neu.ccs.wellness.fitness.MultiDayFitness;
import edu.neu.ccs.wellness.fitness.interfaces.AvailableChallengesInterface;
import edu.neu.ccs.wellness.fitness.interfaces.ChallengeManagerInterface;
import edu.neu.ccs.wellness.fitness.interfaces.OneDayFitnessInterface;
import edu.neu.ccs.wellness.fitness.storage.FitnessRepository;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.server.RestServer.ResponseType;
import edu.neu.ccs.wellness.server.WellnessRestServer;
import edu.neu.ccs.wellness.story.interfaces.StoryInterface;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.utils.WellnessDate;

/**
 * Created by hermansaksono on 5/16/18.
 */

public class ChallengePickerViewModel extends AndroidViewModel {

    private Storywell storywell;
    private final FitnessRepository fitnessRepo;
    private List<Person> familyMembers;
    private Map<Person, Integer> familyMembersStepsAverage = new HashMap<>();
    private MutableLiveData<AvailableChallengesInterface> groupChallengeLiveData;

    public ChallengePickerViewModel(Application application) {
        super(application);
        this.storywell =  new Storywell(getApplication());
        this.familyMembers = storywell.getGroup().getMembers();
        this.fitnessRepo = new FitnessRepository();
    }

    /**
     * Get the list of {@link StoryInterface}.
     * @return
     */
    public LiveData<AvailableChallengesInterface> getGroupChallenges() {
        if (this.groupChallengeLiveData == null) {
            this.groupChallengeLiveData = new MutableLiveData<>();
            //this.loadChallenges();
            this.fetchDailyFitnessFromTodayThenLoadChallenges();
        }
        return this.groupChallengeLiveData;
    }

    private void fetchDailyFitnessFromTodayThenLoadChallenges() {
        Calendar startCal = WellnessDate.getBeginningOfDay();
        startCal.add(Calendar.DATE, -7);
        Calendar endCal = WellnessDate.getBeginningOfDay();
        Iterator<Person> personIterator = this.familyMembers.iterator();

        this.iterateFetchPersonDailyFitness(personIterator, startCal.getTime(), endCal.getTime());
    }

    private void iterateFetchPersonDailyFitness(
            Iterator<Person> personIterator, Date startDate, Date endDate) {
        if (personIterator.hasNext()) {
            this.fetchPersonDailyFitness(personIterator, startDate, endDate);
        } else {
            this.onCompletedPersonDailyFitnessIteration();
        }
    }

    private void fetchPersonDailyFitness(final Iterator<Person> personIterator,
                                         final Date startDate, final Date endDate) {
        final Person person = personIterator.next();

        this.fitnessRepo.fetchDailyFitness(person, startDate, endDate, new ValueEventListener(){
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        MultiDayFitness multiDayFitness = FitnessRepository
                                .getMultiDayFitness(startDate, endDate, dataSnapshot);
                        int stepsAverage = getStepsAverage(multiDayFitness);
                        familyMembersStepsAverage.put(person, stepsAverage);
                        iterateFetchPersonDailyFitness(personIterator, startDate, endDate);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        new LoadChallengesAsync(1000, 1000).execute();
                    }
                });
    }

    private int getStepsAverage(MultiDayFitness multiDayFitness) {
        int totalSteps = 0;
        int totalDays = 0;
        for (OneDayFitnessInterface oneDayFitness : multiDayFitness.getDailyFitness()) {
            totalSteps += oneDayFitness.getSteps();
            totalDays += 1;
        }
        return Math.round(totalSteps / totalDays);
    }

    private void onCompletedPersonDailyFitnessIteration() {
        int adultStepsAverage = this.familyMembersStepsAverage.get(storywell.getCaregiver());
        int childStepsAverage = this.familyMembersStepsAverage.get(storywell.getChild());
        new LoadChallengesAsync(adultStepsAverage, childStepsAverage).execute();
    }

    private class LoadChallengesAsync extends AsyncTask<Void, Integer, ResponseType> {
        int adultStepsAverage;
        int childStepsAverage;

        public LoadChallengesAsync(int adultStepsAverage, int childStepsAverage) {
            this.adultStepsAverage = adultStepsAverage;
            this.childStepsAverage = childStepsAverage;
        }

        protected ResponseType doInBackground(Void... voids) {
            if (WellnessRestServer.isServerOnline(getApplication()) == false) {
                return ResponseType.NO_INTERNET;
            }

            try {
                ChallengeManagerInterface challengeManager = storywell.getChallengeManager();
                AvailableChallengesInterface availableChallenges = challengeManager
                        .getAvailableChallenges(this.adultStepsAverage, this.childStepsAverage);

                groupChallengeLiveData.postValue(availableChallenges);
                return ResponseType.SUCCESS_202;
            } catch (JSONException e) {
                e.printStackTrace();
                return ResponseType.BAD_JSON;
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseType.BAD_REQUEST_400;
            }
        }

        protected void onPostExecute(ResponseType result) {
            switch (result) {
                case SUCCESS_202:
                    Log.d("SWELL", "ChallengePicker loaded this challenge: " +
                            result.toString());
                    break;
                default:
                    break;
            }
        }

    }
}

package edu.neu.ccs.wellness.storytelling.storyview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.interfaces.GroupChallengeInterface;
import edu.neu.ccs.wellness.server.RestServer;
import edu.neu.ccs.wellness.server.WellnessRestServer;
import edu.neu.ccs.wellness.server.WellnessUser;
import edu.neu.ccs.wellness.storytelling.models.challenges.AvailableChallenge;
import edu.neu.ccs.wellness.storytelling.models.challenges.GroupChallenge;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.utils.OnGoToFragmentListener.TransitionType;

/**
 * Created by hermansaksono on 6/25/17.
 */

public class ChallengePickerFragment extends Fragment {
    private static final String STORY_TEXT_FACE = "fonts/pangolin_regular.ttf";
    private View view;
    private GroupChallenge groupChallenge = new GroupChallenge();

    private OnGoToFragmentListener mOnGoToFragmentListener;

    public ChallengePickerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.fragment_challenge_picker, container, false);
        View buttonNext = view.findViewById(R.id.buttonNext);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSubmitPickedChallenges();
            }
        });
        new AsyncLoadChallenges().execute();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnGoToFragmentListener = (OnGoToFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement OnReflectionBeginListener");
        }
    }


    // PRIVATE ASYNCTASK SUBCLASSES
    private class AsyncLoadChallenges extends AsyncTask<Void, Integer, RestServer.ResponseType> {

        protected RestServer.ResponseType doInBackground(Void... voids) {
            WellnessUser user = new WellnessUser(Storywell.DEFAULT_USER, Storywell.DEFAULT_PASS);
            WellnessRestServer server = new WellnessRestServer(Storywell.SERVER_URL, 0, Storywell.API_PATH, user);
            if (server.isOnline(getContext()) == false) {
                return RestServer.ResponseType.NO_INTERNET;
            }
            else {
                return groupChallenge.loadChallenges(getContext(), server);
            }
        }

        protected void onPostExecute(RestServer.ResponseType result) {
            if (result == RestServer.ResponseType.NO_INTERNET) {
                Log.d("WELL Challenges d/l", result.toString());
            }
            else if (result == RestServer.ResponseType.NOT_FOUND_404) {
                Log.d("WELL Challenges d/l", result.toString());
            }
            else if (result == RestServer.ResponseType.SUCCESS_202) {
                Log.d("WELL Challenges d/l", groupChallenge.toString());
                updateView();
            }
        }

    }

    private class AsyncPostChallenge extends AsyncTask<AvailableChallenge, Integer, RestServer.ResponseType> {

        private GroupChallenge runningChallenge = new GroupChallenge();

        protected RestServer.ResponseType doInBackground(AvailableChallenge... challenges) {
            WellnessUser user = new WellnessUser(Storywell.DEFAULT_USER, Storywell.DEFAULT_PASS);
            WellnessRestServer server = new WellnessRestServer(Storywell.SERVER_URL, 0, Storywell.API_PATH, user);

            if (server.isOnline(getContext()) == false) {
                return RestServer.ResponseType.NO_INTERNET;
            }
            else {
                return runningChallenge.postAvailableChallenge(challenges[0], server);
            }
        }

        protected void onPostExecute(RestServer.ResponseType result) {
            Log.d("WELL Challenge posted", result.toString());
            if (result == RestServer.ResponseType.NO_INTERNET) {
                // TODO
            }
            else if (result == RestServer.ResponseType.NOT_FOUND_404) {
                // TODO
            }
            else if (result == RestServer.ResponseType.SUCCESS_202) {
                // TODO
            }
        }

    }

    private void updateView(){
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), STORY_TEXT_FACE);
        TextView textView = (TextView) view.findViewById(R.id.text);
        TextView subtextView = (TextView) view.findViewById(R.id.subtext);

        textView.setText(groupChallenge.getText());
        textView.setTypeface(tf);
        subtextView.setText(groupChallenge.getSubtext());
        subtextView.setTypeface(tf);

        if (groupChallenge.getStatus() == GroupChallengeInterface.ChallengeStatus.AVAILABLE ) {
            RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.challengesRadioGroup);
            for (int i = 0; i < radioGroup.getChildCount();i ++) {
                RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                radioButton.setText(groupChallenge.getAvailableChallenges().get(i).getText());
                radioButton.setTypeface(tf);
            }
        }
    }

    private void doSubmitPickedChallenges() {
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.challengesRadioGroup);
        int radioButtonId = radioGroup.getCheckedRadioButtonId();
        if (radioButtonId >= 0) {
            RadioButton radioButton = (RadioButton) radioGroup.findViewById(radioButtonId);
            int index = radioGroup.indexOfChild(radioButton);
            AvailableChallenge availableChallenge = groupChallenge.getAvailableChallenges().get(index);
            new AsyncPostChallenge().execute(availableChallenge);
            mOnGoToFragmentListener.onGoToFragment(TransitionType.ZOOM_OUT, 1);
        } else {
            Toast.makeText(getContext(), "Please pick one adventure first", Toast.LENGTH_SHORT).show();
        }
    }
}

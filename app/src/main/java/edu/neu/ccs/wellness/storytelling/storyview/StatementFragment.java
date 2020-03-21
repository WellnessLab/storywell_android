package edu.neu.ccs.wellness.storytelling.storyview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.ccs.wellness.story.StoryStatement;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.StoryViewActivity;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

/**
 * A Fragment to show a simple view of one artwork and one text of the Story.
 */
public class StatementFragment extends Fragment
        implements View.OnClickListener{

    private static final int VIEW_DEFAULT = 0;
    private static final int VIEW_CHILD_MOOD_LOG = 1;
    private static final int VIEW_ADULT_MOOD_LOG = 2;

    private int contentId;
    private boolean isInviteMoodLog = false;

    private ViewAnimator viewAnimator;
    private OnGoToFragmentListener onGoToFragmentCallback;
    private StatementFragmentListener statementFragmentListener;
    private View controlAreaView;

    /* INTERFACE */
    public interface StatementFragmentListener {
        boolean isMoodLogResponded(int contentId);
        void setMoodLogResponded(int contentId);
    }

    public StatementFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.onCreateAdultMoodLogs();

        this.contentId = getArguments().getInt(StoryStatement.KEY_CONTENT_ID, -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statement_view, container, false);
        String textStatement = getArguments().getString("KEY_TEXT");

        TextView tv = view.findViewById(R.id.text);
        tv.setText(textStatement);

        this.isInviteMoodLog = getArguments().getBoolean(StoryStatement.KEY_IS_INVITE_LOG_MOOD);
        this.viewAnimator = view.findViewById(R.id.view_animator);
        this.viewAnimator.setInAnimation(view.getContext(), R.anim.reflection_fade_in);
        this.viewAnimator.setOutAnimation(view.getContext(), R.anim.reflection_fade_out);
        this.controlAreaView = view.findViewById(R.id.control_layout);
        this.doHideOrShowNextButton(statementFragmentListener.isMoodLogResponded(contentId));

        view.findViewById(R.id.button_statement_next).setOnClickListener(this);
        view.findViewById(R.id.button_send_child_moods).setOnClickListener(this);
        view.findViewById(R.id.button_send_adult_moods).setOnClickListener(this);

        this.onCreateViewChildMoodLogs(
                (ViewGroup) view.findViewById(R.id.child_mood_picker_button_group));

        this.onCreateViewAdultMoodLogs(
                (ViewGroup) view.findViewById(R.id.adult_mood_picker_button_group));

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onGoToFragmentCallback = (OnGoToFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement OnGoToFragmentListener");
        }
        try {
            statementFragmentListener = (StatementFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement StatementFragmentListener");
        }
    }

    private void doHideOrShowNextButton(boolean isMoodLogResponded) {
        if (isInviteMoodLog && !isMoodLogResponded) {
            this.controlAreaView.setVisibility(View.VISIBLE);
        } else {
            this.controlAreaView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_statement_next:
                onStatementNextButtonPressed();
                break;
            case R.id.button_send_child_moods:
                onChildMoodsLogSubmit();
                break;
            case R.id.button_send_adult_moods:
                onAdultMoodsLogSubmit();
                break;
        }
    }

    private void onStatementNextButtonPressed() {
        if (this.isInviteMoodLog) {
            viewAnimator.setDisplayedChild(VIEW_CHILD_MOOD_LOG);
        } else {
            onGoToFragmentCallback.onGoToFragment(
                    OnGoToFragmentListener.TransitionType.ZOOM_OUT, 1);
        }
    }

    private void onChildMoodsLogSubmit() {
        this.doSubmitChildMoods();
        this.viewAnimator.setDisplayedChild(VIEW_ADULT_MOOD_LOG);
    }

    private void onAdultMoodsLogSubmit() {
        this.doSubmitAdultMoods();
        this.onGoToFragmentCallback.onGoToFragment(
                OnGoToFragmentListener.TransitionType.ZOOM_OUT, 1);
        this.viewAnimator.setDisplayedChild(VIEW_DEFAULT);
        this.statementFragmentListener.setMoodLogResponded(contentId);
    }

    /* CHILD MOOD LOGGING FIELDS AND METHODS */
    private List<ToggleButton> toggleButtonListChild= new ArrayList<>();

    private void onCreateViewChildMoodLogs(ViewGroup moodButtonGroup) {
        for (int i = 0; i < moodButtonGroup.getChildCount(); i++) {
            ToggleButton toggleButton = (ToggleButton) moodButtonGroup.getChildAt(i);
            toggleButtonListChild.add(toggleButton);
        }
    }

    private void doSubmitChildMoods() {
        List<String> loggedMood = new ArrayList<>();

        for (int i = 0; i < toggleButtonListChild.size(); i++) {
            ToggleButton toggleButton = toggleButtonListChild.get(i);
            if (toggleButton.isChecked()) {
                loggedMood.add(getChildEmotionString(toggleButton));
            }
        }

        UserLogging.logChildEmotion(getEmotionJsonString(loggedMood));
    }

    /* ADULT MOOD LOGGING FIELDS AND METHODS */
    private Map<String, Integer> emotionIdMap = new HashMap<>();
    private Map<Integer, String> idEmotionMap = new HashMap<>();
    private List<Integer> reorderedMoods = new ArrayList<>();
    private List<ToggleButton> toggleButtonListAdult = new ArrayList<>();

    // Populate the maps to lookup emotions
    private void onCreateAdultMoodLogs() {
        int emotionId = 0;
        for (String emotion : getResources().getStringArray(R.array.panas_positive_emotion_list)) {
            emotionIdMap.put(emotion, emotionId);
            idEmotionMap.put(emotionId, emotion);
            reorderedMoods.add(emotionId);
            emotionId++;
        }
        for (String emotion : getResources().getStringArray(R.array.panas_negative_emotion_list)) {
            emotionIdMap.put(emotion, emotionId);
            idEmotionMap.put(emotionId, emotion);
            reorderedMoods.add(emotionId);
            emotionId++;
        }

        // Collections.shuffle(reorderedMoods);
    }

    private void onCreateViewAdultMoodLogs(ViewGroup moodButtonGroup) {
        for (int i = 0; i < moodButtonGroup.getChildCount(); i++) {
            int buttonTextId = reorderedMoods.get(i);
            String buttonText = idEmotionMap.get(buttonTextId);
            ToggleButton toggleButton = (ToggleButton) moodButtonGroup.getChildAt(i);
            toggleButton.setText(buttonText);
            toggleButton.setTextOff(buttonText);
            toggleButton.setTextOn(buttonText);

            toggleButtonListAdult.add(toggleButton);
        }
    }

    private void doSubmitAdultMoods() {
        List<String> loggedMood = new ArrayList<>();

        for (int i = 0; i < toggleButtonListAdult.size(); i++) {
            ToggleButton toggleButton = toggleButtonListAdult.get(i);
            if (toggleButton.isChecked()) {
                loggedMood.add((String) toggleButton.getText());
            }
        }

        UserLogging.logAdultEmotion(getEmotionJsonString(loggedMood));
    }


    /* HELPER METHODS */
    private static String getEmotionJsonString(List<String> loggedMood) {
        return new Gson().toJson(loggedMood);
    }

    private String getChildEmotionString(ToggleButton toggleButton) {
        switch (toggleButton.getId()) {
            case R.id.child_happy:
                return getString(R.string.child_mood_happy);
            case R.id.child_laugh:
                return getString(R.string.child_mood_laugh);
            case R.id.child_sad:
                return getString(R.string.child_mood_sad);
            case R.id.child_angry:
                return getString(R.string.child_mood_angry);
            default:
                return "";
        }
    }
}



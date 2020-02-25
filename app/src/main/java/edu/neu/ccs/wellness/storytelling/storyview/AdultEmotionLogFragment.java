package edu.neu.ccs.wellness.storytelling.storyview;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

/**
 * A Fragment that allows adults to log their emotions
 */
public class AdultEmotionLogFragment extends Fragment {

    private Map<String, Integer> emotionIdMap = new HashMap<>();
    private Map<Integer, String> idEmotionMap = new HashMap<>();
    private List<Integer> reorderedMoods = new ArrayList<>();


    private List<ToggleButton> toggleButtonList= new ArrayList<>();

    public AdultEmotionLogFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate the maps to lookup emotions
        int emotionId = 0;
        for (String emotion : getResources().getStringArray(R.array.panas_emotion_list)) {
            emotionIdMap.put(emotion, emotionId);
            idEmotionMap.put(emotionId, emotion);
            reorderedMoods.add(emotionId);
            emotionId++;
        }

        Collections.shuffle(reorderedMoods);
    }

    /**
     * The system calls onCreateView when it's time for the fragment to draw its user interface
     * for the first time.
     * So the view gets inflated which is considered one of the most heavy tasks in Android
     * Do all essential initializations in onCreate of Fragments above
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.layout_mood_picker_adult, container, false);

        // Set up the mood buttons
        ViewGroup moodButtonGroup = view.findViewById(R.id.adult_mood_picker_button_group);

        for (int i = 0; i < moodButtonGroup.getChildCount(); i++) {
            String buttonText = idEmotionMap.get(i);
            ToggleButton toggleButton = (ToggleButton) moodButtonGroup.getChildAt(i);
            toggleButton.setTextOn(buttonText);
            toggleButton.setTextOff(buttonText);

            toggleButtonList.add(toggleButton);
        }

        // Set up the button to send moods
        Button sendEmotionsButton = view.findViewById((R.id.button_send_adult_moods));
        sendEmotionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitEmotionLog();
            }
        });

        return view;
    }

    private void submitEmotionLog() {
        List<String> loggedMood = new ArrayList<>();

        for (int i = 1; i <= toggleButtonList.size(); i++) {
            ToggleButton toggleButton = toggleButtonList.get(i);
            if (toggleButton.isChecked()) {
                loggedMood.add(idEmotionMap.get(i));
            }
        }

        UserLogging.logAdultEmotion(getEmotionJsonString(loggedMood));
    }

    private static String getEmotionJsonString(List<String> loggedMood) {
        return new Gson().toJson(loggedMood);
    }

}
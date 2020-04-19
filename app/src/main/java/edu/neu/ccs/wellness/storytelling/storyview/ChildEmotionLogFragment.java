package edu.neu.ccs.wellness.storytelling.storyview;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;

/**
 * A Fragment that allows adults to log their emotions
 */
public class ChildEmotionLogFragment extends Fragment {

    private List<ToggleButton> toggleButtonList= new ArrayList<>();

    public ChildEmotionLogFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            ToggleButton toggleButton = (ToggleButton) moodButtonGroup.getChildAt(i);
            toggleButtonList.add(toggleButton);
        }

        // Set up the button to send moods
        Button sendEmotionsButton = view.findViewById((R.id.button_send_child_moods));
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
                loggedMood.add(getChildEmotionString(toggleButton));
            }
        }

        UserLogging.logChildEmotion(getEmotionJsonString(loggedMood));
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

    private static String getEmotionJsonString(List<String> loggedMood) {
        return new Gson().toJson(loggedMood);
    }

}
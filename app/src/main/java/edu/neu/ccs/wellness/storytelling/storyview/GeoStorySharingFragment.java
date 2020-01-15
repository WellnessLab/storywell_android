package edu.neu.ccs.wellness.storytelling.storyview;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;

import edu.neu.ccs.wellness.story.GeoStorySharing;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.storytelling.utils.StoryContentAdapter;

/**
 * Recording and Playback of Audio
 * For reference use Android Docs
 * https://developer.android.com/guide/topics/media/mediarecorder.html
 */
public class GeoStorySharingFragment extends Fragment implements View.OnClickListener {


    /***************************************************************************
     * VARIABLE DECLARATION
     ***************************************************************************/

    private static final int CONTROL_BUTTON_OFFSET = 10;
    private static final Boolean DEFAULT_IS_RESPONSE_STATE = false;

    private View view;
    private ViewAnimator mainViewAnimator;
    private OnGoToFragmentListener onGoToFragmentCallback;
    private GeoStoryFragmentListener geoStoryFragmentListener;

    private String promptId;
    private String promptSubId;

    private ImageButton buttonReplay;
    private TextView textViewReplay;
    private ImageButton buttonRespond;
    private TextView textViewRespond;
    private Button buttonBack;
    private Button buttonNext;

    private Drawable playDrawable;
    private Drawable stopDrawable;



    /**
     * Ask for Audio Permissions
     */
    private static final int REQUEST_AUDIO_PERMISSIONS = 100;
    private String[] permission = {Manifest.permission.RECORD_AUDIO};

    /**
     * Audio File Name
     * Made Static as it will be used in uploading to Firebase
     */

    //Initialize the MediaRecorder for Reflections Recording
    private Boolean isResponding = false;
    private boolean isResponseExists;

    public View recordingProgressBar;
    public View playbackProgressBar;
    private float controlButtonVisibleTranslationY;
    private boolean isRecording;

    private Boolean isPlayingRecording = false;

    private String dateString = null;


    public GeoStorySharingFragment() {
    }

    public interface GeoStoryFragmentListener {
        boolean isGeoStoryExists(String promptSubId);
        void doStartGeoStoryRecording(String promptId, String promptSubId);
        void doStopGeoStoryRecording();
        void doStartGeoStoryPlay(String promptId, OnCompletionListener completionListener);
        void doStopGeoStoryPlay();
    }

    /**
     * Initialization should be done here
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.promptId = getArguments().getString(GeoStorySharing.KEY_PROMPT_ID);
        this.promptSubId = getArguments().getString(StoryContentAdapter.KEY_ID);
        this.view = getView(inflater, container);
        this.mainViewAnimator = getMainViewAnim(this.view);

        this.playDrawable = getResources().getDrawable(R.drawable.ic_round_play_arrow_big);
        this.stopDrawable = getResources().getDrawable(R.drawable.ic_round_stop_big);

        this.buttonRespond = view.findViewById(R.id.button_respond);
        this.buttonBack = view.findViewById(R.id.button_back);
        this.buttonNext = view.findViewById(R.id.button_share);
        this.buttonReplay = view.findViewById(R.id.button_play);
        this.textViewRespond = view.findViewById(R.id.text_respond);
        this.textViewReplay = view.findViewById(R.id.textPlay);
        this.recordingProgressBar = view.findViewById(R.id.recording_progress_bar);
        this.playbackProgressBar = view.findViewById(R.id.playback_progress_bar);

        this.controlButtonVisibleTranslationY = buttonNext.getTranslationY();

        if (getArguments().containsKey(StoryContentAdapter.KEY_REFLECTION_DATE)) {
            TextView dateTextView = view.findViewById(R.id.reflectionDate);
            dateTextView.setVisibility(View.VISIBLE);
            dateTextView.setText(getArguments().getString(StoryContentAdapter.KEY_REFLECTION_DATE));
            view.findViewById(R.id.reflectionInstruction).setVisibility(View.GONE);
        }

        /**Get the text to display from bundle and show it as view*/
        String text = getArguments().getString(StoryContentAdapter.KEY_TEXT);
        String subtext = getArguments().getString(StoryContentAdapter.KEY_SUBTEXT);
        setContentText(view, text, subtext);

        buttonNext.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {

            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        return view;
    }

    private static View getView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.fragment_geostory_share, container, false);
    }

    private static ViewFlipper getMainViewAnim(View view) {
        ViewFlipper viewFlipper = view.findViewById(R.id.main_view_animator);
        viewFlipper.setInAnimation(view.getContext(), R.anim.reflection_fade_in);
        viewFlipper.setOutAnimation(view.getContext(), R.anim.reflection_fade_out);
        return viewFlipper;
    }

    /***
     * Set View to show the Story's content
     * @param view The View in which the content will be displayed
     * @param text The prompt's text
     * @param subtext The prompt's extra text
     */
    private static void setContentText(View view, String text, String subtext) {
        TextView tv = view.findViewById(R.id.reflectionText);
        TextView stv = view.findViewById(R.id.reflectionSubtext);

        tv.setText(text);
        stv.setText(subtext);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_respond:
                onRespondButtonPressed(getActivity(), view);
                break;
            case R.id.button_play:
                onReplayButtonPressed();
                break;
            case R.id.button_share:
                onShareButtonPressed();
                break;
            case R.id.button_edit:
                onButtonEditPressed();
                break;
            case R.id.button_back:
                onButtonBackPressed(getContext());
                break;
            default:
                break;
        }
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
            geoStoryFragmentListener = (GeoStoryFragmentListener) context;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement GeoStoryFragmentListener");
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            this.isResponseExists = savedInstanceState.getBoolean(
                    StoryContentAdapter.KEY_IS_RESPONSE_EXIST, DEFAULT_IS_RESPONSE_STATE);
        } else {
            this.isResponseExists = geoStoryFragmentListener.isGeoStoryExists(promptSubId);
        }


        changeButtonsVisibility(this.isResponseExists);
        changeReflectionStartVisibility(this.isResponseExists, this.mainViewAnimator);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(StoryContentAdapter.KEY_IS_RESPONSE_EXIST, isResponseExists);
    }


    @Override
    public void onPause() {
        super.onPause();
        /**If Recording if not stopped and someone minimizes the app, stop the recording*/
        if (isRecording) {
            buttonRespond.performClick();
        }

        SharedPreferences saveStateStoryPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor saveStateStory = saveStateStoryPref.edit();
        //TODO: Remove these states from here (Giving inconsistent results)
        //Do it in StoryViewActivity
//        saveStateStory.putInt("PAGE ID", promptSubId);
//        saveStateStory.putString("REFLECTION URL", getStoryCallback.getStoryState().getState().getRecordingURL(promptSubId));
        saveStateStory.apply();
    }


    @Override
    public void onResume() {
        super.onResume();
        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        //TODO: Remove this from here (Giving inconsistent results)
        //Do it in StoryViewActivity
//        this.story.getState().addReflection(pref.getInt("PAGE ID", promptSubId), pref.getString("REFLECTION URL", " "));
    }

    private void changeReflectionStartVisibility(boolean isResponseExists, ViewAnimator viewAnim) {
        if (isResponseExists) {
            viewAnim.showNext();
        }
    }

    private static void changeReflectionEditButtonVisibility(boolean isAllowEdit, View view) {
        if (!isAllowEdit) {
            view.setVisibility(View.INVISIBLE);
        }
    }


    /***************************************************************
     * METHODS TO ANIMATE BUTTONS
     ***************************************************************/
    public void onReplayButtonPressed() {
        if (isPlayingRecording == false) {
            this.startPlayingResponse();
        } else {
            this.stopPlayingResponse();
        }
    }

    private void startPlayingResponse() {
        OnCompletionListener onCompletionListener = new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlayingResponse();
            }
        };

        if (isPlayingRecording == false) {
            this.fadePlaybackProgressBarTo(1, R.integer.anim_short);
            this.geoStoryFragmentListener.doStartGeoStoryPlay(promptSubId, onCompletionListener);
            //this.buttonReplay.setText(R.string.reflection_button_replay_stop);
            this.textViewReplay.setText(R.string.reflection_label_playing);
            this.buttonReplay.setImageDrawable(stopDrawable);
            this.isPlayingRecording = true;
        }
    }

    private void stopPlayingResponse() {
        if (isPlayingRecording == true && this.getActivity() != null ) {
            this.fadePlaybackProgressBarTo(0, R.integer.anim_short);
            this.geoStoryFragmentListener.doStopGeoStoryPlay();
            //this.buttonReplay.setText(R.string.reflection_button_replay);
            this.textViewReplay.setText(R.string.reflection_label_play);
            this.buttonReplay.setImageDrawable(playDrawable);
            this.isPlayingRecording = false;
        }
    }

    public void onRespondButtonPressed(Context context, View view) {
        if (isRecordingAllowed() == false) {
            requestPermissions(permission, REQUEST_AUDIO_PERMISSIONS);
            return;
        }
        if (isResponding) {
            this.stopResponding();
        } else {
            this.startResponding();
        }
    }

    private void startResponding() {
        this.isResponding = true;
        this.fadeRecordingProgressBarTo(1, R.integer.anim_short);
        //this.fadeControlButtonsTo(view, 0);
        //this.changeReflectionButtonTextTo(getString(R.string.reflection_button_stop));
        this.textViewRespond.setText(getString(R.string.reflection_label_record));

        this.geoStoryFragmentListener.doStartGeoStoryRecording(this.promptId, this.promptSubId);
    }

    private void stopResponding() {
        this.geoStoryFragmentListener.doStopGeoStoryRecording();

        this.isResponding = false;
        this.fadeRecordingProgressBarTo(0, R.integer.anim_fast);
        //this.changeReflectionButtonTextTo(getString(R.string.reflection_button_answer));
        //this.fadeControlButtonsTo(view, 1);
        this.textViewRespond.setText(getString(R.string.reflection_label_answer));
        this.doGoToPlaybackControl();
    }

    private void doGoToPlaybackControl() {
        this.mainViewAnimator.setInAnimation(getContext(), R.anim.view_move_left_next);
        this.mainViewAnimator.setOutAnimation(getContext(), R.anim.view_move_left_current);
        this.mainViewAnimator.showNext();
    }

    private void onButtonBackPressed(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.reflection_delete_confirmation_title);
        builder.setMessage(R.string.reflection_delete_confirmation_desc);
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                doGoToRecordingControl();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void doGoToRecordingControl() {
        this.geoStoryFragmentListener.doStopGeoStoryPlay();
        this.mainViewAnimator.setInAnimation(getContext(), R.anim.view_move_right_prev);
        this.mainViewAnimator.setOutAnimation(getContext(), R.anim.view_move_right_current);
        this.mainViewAnimator.showPrevious();
    }

    private void onShareButtonPressed() {
        onGoToFragmentCallback.onGoToFragment(
                OnGoToFragmentListener.TransitionType.ZOOM_OUT, 1);
    }

    private void onButtonEditPressed() {

    }

    private void fadeRecordingProgressBarTo(float alpha, int animLengthResId) {
        recordingProgressBar.animate()
                .alpha(alpha)
                .setDuration(getResources().getInteger(animLengthResId))
                .setListener(null);
    }

    private void fadePlaybackProgressBarTo(float alpha, int animLengthResId) {
        playbackProgressBar.animate()
                .alpha(alpha)
                .setDuration(getResources().getInteger(animLengthResId))
                .setListener(null);
    }

    private void fadeControlButtonsTo(View view, float toAlpha) {
        buttonNext.animate()
                .alpha(toAlpha)
                .translationY(getControlButtonOffset(toAlpha))
                .setDuration(getResources().getInteger(R.integer.anim_fast))
                .setListener(new FadeSwitchListener(toAlpha));
        buttonReplay.animate()
                .alpha(toAlpha)
                .translationY(getControlButtonOffset(toAlpha))
                .setDuration(getResources().getInteger(R.integer.anim_fast))
                .setListener(new FadeSwitchListener(toAlpha));
    }

    private float getControlButtonOffset(float toAlpha) {
        return controlButtonVisibleTranslationY + (CONTROL_BUTTON_OFFSET * (1 - toAlpha));
    }

    public class FadeSwitchListener extends AnimatorListenerAdapter {
        private float toAlpha;

        public FadeSwitchListener(float toAlpha) {
            this.toAlpha = toAlpha;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (toAlpha > 0) {
                buttonNext.setVisibility(View.VISIBLE);
                buttonReplay.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (toAlpha <= 0) {
                buttonNext.setVisibility(View.GONE);
                buttonReplay.setVisibility(View.GONE);
            }
        }
    }

    /***************************************************************************
     * If Recordings are available in either state or either in Firebase
     * Then make the buttons visible
     ***************************************************************************/
    private void changeButtonsVisibility(boolean isResponseExists) {
        if (isResponseExists) {
            mainViewAnimator.showNext();
        }
    }

    private boolean isRecordingAllowed() {
        int permissionRecordAudio = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO);
        return permissionRecordAudio == PackageManager.PERMISSION_GRANTED;
    }


}
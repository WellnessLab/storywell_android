package edu.neu.ccs.wellness.storytelling.storyview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.neu.ccs.wellness.fitness.MultiDayFitness;
import edu.neu.ccs.wellness.fitness.storage.FitnessRepository;
import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.story.GeoStorySharing;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.storytelling.utils.StoryContentAdapter;
import edu.neu.ccs.wellness.utils.WellnessDate;

/**
 * Recording and Playback of Audio
 * For reference use Android Docs
 * https://developer.android.com/guide/topics/media/mediarecorder.html
 */
public class GeoStorySharingFragment extends Fragment
        implements View.OnClickListener, EditGeoStoryMetaDialogFragment.GeoStoryMetaListener {


    /***************************************************************************
     * VARIABLE DECLARATION
     ***************************************************************************/

    private static final int CONTROL_BUTTON_OFFSET = 10;
    private static final Boolean DEFAULT_IS_RESPONSE_STATE = false;

    private Storywell storywell;

    private View view;
    private ViewAnimator mainViewAnimator;
    private OnGoToFragmentListener onGoToFragmentCallback;
    private GeoStoryFragmentListener geoStoryFragmentListener;

    private FusedLocationProviderClient fusedLocationClient;

    private String promptParentId;
    private String promptId;

    private ImageButton buttonReplay;
    private TextView textViewReplay;
    private ImageButton buttonRespond;
    private TextView textViewRespond;
    private TextView textViewAvgSteps;
    private TextView textViewNeighborhood;
    private TextView textViewBio;

    private Drawable playDrawable;
    private Drawable stopDrawable;

    private Location geoLocation;
    private GeoStoryMeta geoStoryMeta = new GeoStoryMeta();
    private Address geoStoryAddress = new Address(Locale.US);

    private OnSuccessListener<Location> locationListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            geoLocation = location;
            fetchAddress(geoLocation);
        }
    };

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
    private boolean isRecording;

    private Boolean isPlayingRecording = false;

    private String dateString = null;


    public GeoStorySharingFragment() {
    }

    public interface GeoStoryFragmentListener {
        boolean isGeoStoryExists(String promptId);
        void doStartGeoStoryRecording(String promptParentId, String promptId);
        void doStopGeoStoryRecording();
        void doStartGeoStoryPlay(String promptId, OnCompletionListener completionListener);
        void doStopGeoStoryPlay();
        boolean doShareGeoStory(Location location, GeoStoryMeta geoStoryMeta);
        FusedLocationProviderClient getLocationProvider();
    }

    /**
     * Initialization should be done here
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.storywell = new Storywell(this.getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.promptParentId = getArguments().getString(GeoStorySharing.KEY_PROMPT_PARENT_ID);
        this.promptId = getArguments().getString(StoryContentAdapter.KEY_ID);
        this.geoStoryMeta.setPromptParentId(this.promptParentId);
        this.geoStoryMeta.setPromptId(this.promptId);
        this.geoStoryMeta.setBio(this.storywell.getSynchronizedSetting().getCaregiverGio());

        this.view = getView(inflater, container);
        this.mainViewAnimator = getMainViewAnim(this.view);

        this.playDrawable = getResources().getDrawable(R.drawable.ic_round_play_arrow_big);
        this.stopDrawable = getResources().getDrawable(R.drawable.ic_round_stop_big);

        this.buttonRespond = view.findViewById(R.id.button_respond);
        this.buttonReplay = view.findViewById(R.id.button_play);
        this.textViewRespond = view.findViewById(R.id.text_respond);
        this.textViewReplay = view.findViewById(R.id.textPlay);
        this.textViewAvgSteps = view.findViewById(R.id.average_steps);
        this.textViewNeighborhood = view.findViewById(R.id.neighborhood);
        this.textViewBio = view.findViewById(R.id.user_bio);
        this.recordingProgressBar = view.findViewById(R.id.recording_progress_bar);
        this.playbackProgressBar = view.findViewById(R.id.playback_progress_bar);

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

        this.textViewBio.setText(this.geoStoryMeta.getBio());

        this.fetchCaregiverAverageSteps();

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
            fusedLocationClient = geoStoryFragmentListener.getLocationProvider();
            this.setLocationListener();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement GeoStoryFragmentListener");
        }

    }

    private void setLocationListener() {
        if (ActivityCompat.checkSelfPermission(
                this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(
                    this.getActivity(), locationListener);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            this.isResponseExists = savedInstanceState.getBoolean(
                    StoryContentAdapter.KEY_IS_RESPONSE_EXIST, DEFAULT_IS_RESPONSE_STATE);
        } else {
            this.isResponseExists = geoStoryFragmentListener.isGeoStoryExists(promptId);
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
        saveStateStory.apply();
    }


    @Override
    public void onResume() {
        super.onResume();
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

    @Override
    public void setEditGeoStoryMeta(GeoStoryMeta geoStoryMeta) {
        this.geoStoryMeta.setShowAverageSteps(geoStoryMeta.isShowAverageSteps());
        this.geoStoryMeta.setShowNeighborhood(geoStoryMeta.isShowNeighborhood());
        this.geoStoryMeta.setBio(geoStoryMeta.getBio());
    }

    public Location getGeoLocation () {
        return this.geoLocation;
    }

    public void setGeoStoryAddress(Address address) {
        this.geoStoryAddress = address;
        this.geoStoryMeta.setNeighborhood(address.getLocality());
        this.textViewNeighborhood.setText(this.geoStoryAddress.getLocality());
    }

    private void fetchAddress(Location location) {
        new ReverseGeocodingTask(this).execute(location);
    }

    private static class ReverseGeocodingTask extends AsyncTask<Location, Void, Address> {

        private static final int MAX_RESULTS = 1;
        private static final int DELAY = 500;

        GeoStorySharingFragment mFragment;

        public ReverseGeocodingTask(GeoStorySharingFragment fragment) {
            this.mFragment = fragment;
        }

        @Override
        protected Address doInBackground(Location... params) {
            Geocoder geocoder = new Geocoder(mFragment.getContext());
            Location location = mFragment.getGeoLocation();
            List<Address> listOfAddress;
            Address address = new Address(Locale.US);

            try {
                listOfAddress = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(),MAX_RESULTS);
                Thread.sleep(DELAY);


                if(listOfAddress != null && listOfAddress.size() > 0 ){
                    address = listOfAddress.get(0);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return address;
        }

        @Override
        protected void onPostExecute(Address address) {
            mFragment.setGeoStoryAddress(address);
        }
    }

    /* CAREGIVER'S AVERAGE STEPS */
    private void setAverageSteps(int stepsAverage) {
        this.geoStoryMeta.setAverageSteps(stepsAverage);
        this.textViewAvgSteps.setText(String.valueOf(stepsAverage));
    }

    private void fetchCaregiverAverageSteps() {
        Calendar startCal = WellnessDate.getBeginningOfDay();
        Calendar endCal = WellnessDate.getBeginningOfDay();

        startCal.add(Calendar.DATE, -8);
        endCal.add(Calendar.DATE, -1);
        Person caregiver = storywell.getCaregiver();

        final Date startDate = startCal.getTime();
        final Date endDate = endCal.getTime();

        FitnessRepository fitnessRepository = new FitnessRepository();
        fitnessRepository.fetchDailyFitness(caregiver, startDate, endDate, new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MultiDayFitness multiDayFitness = FitnessRepository
                        .getMultiDayFitness(startDate, endDate, dataSnapshot);
                setAverageSteps(multiDayFitness.getStepsAverage());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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
            this.geoStoryFragmentListener.doStartGeoStoryPlay(promptId, onCompletionListener);
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

        this.geoStoryFragmentListener.doStartGeoStoryRecording(this.promptParentId, this.promptId);
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
        this.geoStoryFragmentListener.doShareGeoStory(geoLocation, geoStoryMeta);
    }

    private void onButtonEditPressed() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(EditGeoStoryMetaDialogFragment.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        // Create and show the dialog.
        DialogFragment newFragment = EditGeoStoryMetaDialogFragment.newInstance(geoStoryMeta);
        newFragment.show(ft, EditGeoStoryMetaDialogFragment.TAG);
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
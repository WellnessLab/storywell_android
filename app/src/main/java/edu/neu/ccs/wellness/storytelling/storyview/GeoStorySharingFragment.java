package edu.neu.ccs.wellness.storytelling.storyview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener.TransitionType;
import edu.neu.ccs.wellness.storytelling.utils.StoryContentAdapter;
import edu.neu.ccs.wellness.utils.WellnessDate;


public class GeoStorySharingFragment extends Fragment implements
        View.OnClickListener,
        EditGeoStoryMetaDialogFragment.GeoStoryMetaListener,
        OnMapReadyCallback {

    /* CONSTANTS */
    private static final Boolean DEFAULT_IS_RESPONSE_STATE = false;
    private static final int REQUEST_AUDIO_PERMISSIONS = 100;
    private static String[] permission = {Manifest.permission.RECORD_AUDIO}; // For audio permission

    /* FIELDS */
    private Storywell storywell;
    private OnGoToFragmentListener onGoToFragmentCallback;
    private GeoStoryFragmentListener geoStoryFragmentListener;
    private FusedLocationProviderClient fusedLocationClient;

    private View view;
    private ViewAnimator mainViewAnimator;

    private ImageButton buttonReplay;
    private ImageButton buttonRespond;
    private ProgressBar recordingProgressBar;
    private ProgressBar playbackProgressBar;
    private TextView textViewRespond;
    private TextView textViewAvgSteps;
    private TextView textViewNeighborhood;
    private TextView textViewBio;

    private Drawable playDrawable;
    private Drawable stopDrawable;

    private GoogleMap storyGoogleMap;
    private Marker geoLocationMarker;

    private Location geoLocation;
    private GeoStoryMeta geoStoryMeta;
    private Address geoStoryAddress = new Address(Locale.US);
    private String promptParentId;
    private String promptId;

    private boolean isResponding = false;
    private boolean isResponseExists;
    private boolean isPlaying = false;

    /**
     * Listener that must be implemented by the {@link Activity} that uses this Fragment.
     */
    public interface GeoStoryFragmentListener {
        boolean isGeoStoryExists(String promptId);
        void doStartGeoStoryRecording(String promptParentId, String promptId);
        void doStopGeoStoryRecording();
        void doStartGeoStoryPlay(String promptId, OnCompletionListener completionListener);
        void doStopGeoStoryPlay();
        boolean doShareGeoStory(Location location, GeoStoryMeta geoStoryMeta);
        FusedLocationProviderClient getLocationProvider();
    }

    /* CONSTRUCTORS */
    public GeoStorySharingFragment() {
        this.geoStoryMeta = new GeoStoryMeta();
    }

    /* METHODS */
    /**
     * When the Fragment is created but before being attached to the Activity.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.storywell = new Storywell(this.getContext());
    }

    /**
     * Called when the view for the fragment is created.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.promptParentId = getArguments().getString(GeoStorySharing.KEY_PROMPT_PARENT_ID);
        this.promptId = String.valueOf(getArguments().getInt(StoryContentAdapter.KEY_ID));
        this.geoStoryMeta.setPromptParentId(this.promptParentId);
        this.geoStoryMeta.setPromptId(this.promptId);
        this.geoStoryMeta.setBio(this.storywell.getSynchronizedSetting().getCaregiverGio());

        this.playDrawable = getResources().getDrawable(R.drawable.ic_round_play_arrow_big);
        this.stopDrawable = getResources().getDrawable(R.drawable.ic_round_stop_big);

        this.view = inflater.inflate(R.layout.fragment_geostory_share, container, false);
        this.mainViewAnimator = getMainViewAnim(this.view);
        this.buttonRespond = view.findViewById(R.id.button_respond);
        this.buttonReplay = view.findViewById(R.id.button_play);
        this.textViewRespond = view.findViewById(R.id.text_respond);
        this.textViewAvgSteps = view.findViewById(R.id.average_steps);
        this.textViewNeighborhood = view.findViewById(R.id.neighborhood);
        this.textViewBio = view.findViewById(R.id.user_bio);
        this.recordingProgressBar = view.findViewById(R.id.recording_progress_bar);
        this.playbackProgressBar = view.findViewById(R.id.playback_progress_bar);

        // Get the text to display from bundle and show it as view
        String text = getArguments().getString(StoryContentAdapter.KEY_TEXT);
        String subtext = getArguments().getString(StoryContentAdapter.KEY_SUBTEXT);
        setContentText(view, text, subtext);

        this.textViewBio.setText(this.geoStoryMeta.getBio());

        this.view.findViewById(R.id.button_respond_story).setVisibility(View.GONE);

        this.buttonRespond.setOnClickListener(this);
        this.buttonReplay.setOnClickListener(this);

        this.fetchCaregiverAverageSteps();

        return view;
    }

    private static ViewAnimator getMainViewAnim(View view) {
        ViewAnimator viewAnimator = view.findViewById(R.id.main_view_animator);
        viewAnimator.setInAnimation(view.getContext(), R.anim.reflection_fade_in);
        viewAnimator.setOutAnimation(view.getContext(), R.anim.reflection_fade_out);
        return viewAnimator;
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

    /**
     * Handle clicks on the {@link GeoStorySharingFragment} screen.
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_respond:
                onRespondButtonPressed();
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

    /**
     * Called when the fragment is attached to the Activity.
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.onGoToFragmentCallback = (OnGoToFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement OnGoToFragmentListener");
        }

        try {
            this.geoStoryFragmentListener = (GeoStoryFragmentListener) context;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement GeoStoryFragmentListener");
        }

    }

    /**
     * Called when the fragment and the activity has been created.
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            this.isResponseExists = savedInstanceState.getBoolean(
                    StoryContentAdapter.KEY_IS_RESPONSE_EXIST, DEFAULT_IS_RESPONSE_STATE);
        } else {
            this.isResponseExists = geoStoryFragmentListener.isGeoStoryExists(promptId);
        }

        if (this.isResponseExists) {
            mainViewAnimator.showNext();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Called when state's bundle is saved.
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(StoryContentAdapter.KEY_IS_RESPONSE_EXIST, isResponseExists);
    }

    /**
     * Called when the fragment is about to go to the background.
     */
    @Override
    public void onPause() {
        super.onPause();

        // If the app is stopped while recording, stop the recording.
        if (this.isResponding) {
            this.stopResponding();
        }
    }

    /**
     * Called when the {@link GoogleMap} is ready.
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.storyGoogleMap = googleMap;
        this.storyGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        this.showMyLocationMarker();
        this.setLocationListener(this.geoStoryFragmentListener.getLocationProvider());
    }

    private void setLocationListener(FusedLocationProviderClient locationProvider) {
        this.fusedLocationClient = locationProvider;
        if (ActivityCompat.checkSelfPermission(
                this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(
                    this.getActivity(), locationListener);
        }
    }

    /**
     * Listener to handle the GPS location fetching.
     */
    private OnSuccessListener<Location> locationListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(final Location location) {
            geoLocation = StoryMapPresenter.getOffsetLocation(location);
            fetchAddress(geoLocation);

            storyGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    addLocationMarker(geoLocation);
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void showMyLocationMarker() {
        if (StoryMapPresenter.isAccessLocationGranted(getContext())) {
            storyGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void addLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = StoryMapPresenter.getSharingLocationMarker(latLng);
        this.geoLocationMarker = this.storyGoogleMap.addMarker(markerOptions);

        CameraUpdate initialPos = CameraUpdateFactory.newLatLngZoom(latLng, 16);
        this.storyGoogleMap.moveCamera(initialPos);
    }


    /**
     * Given the {@param location}, call the {@link Geocoder} to determine the neighborhood.
     * @param location
     */
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

    protected void setGeoStoryAddress(Address address) {
        this.geoStoryAddress = address;
        this.geoStoryMeta.setNeighborhood(address.getLocality());
        this.textViewNeighborhood.setText(this.geoStoryAddress.getLocality());
    }

    protected Location getGeoLocation () {
        return this.geoLocation;
    }

    /**
     * Handles the metadata change.
     * @param geoStoryMeta
     */
    @Override
    public void setEditGeoStoryMeta(GeoStoryMeta geoStoryMeta) {
        this.geoStoryMeta.setShowAverageSteps(geoStoryMeta.isShowAverageSteps());
        this.geoStoryMeta.setShowNeighborhood(geoStoryMeta.isShowNeighborhood());
        this.geoStoryMeta.setBio(geoStoryMeta.getBio());
    }

    /**
     * Fetch caregiver's average steps from the fitness repository.
     */
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
                MultiDayFitness sevenDayData = FitnessRepository
                        .getMultiDayFitness(startDate, endDate, dataSnapshot);
                int avgSteps = sevenDayData.getStepsAverage();
                geoStoryMeta.setAverageSteps(avgSteps);
                textViewAvgSteps.setText(String.valueOf(avgSteps));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /* BUTTON RELATED METHODS */
    /**
     * Called when the replay button is pressed.
     */
    public void onReplayButtonPressed() {
        if (isPlaying) {
            this.stopPlayingResponse();
        } else {
            this.startPlayingResponse();
        }
    }

    private void startPlayingResponse() {
        OnCompletionListener onCompletionListener = new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlayingResponse();
            }
        };

        if (!isPlaying) {
            this.fadePlaybackProgressBarTo(1, R.integer.anim_short);
            this.geoStoryFragmentListener.doStartGeoStoryPlay(promptId, onCompletionListener);
            this.buttonReplay.setImageDrawable(stopDrawable);
            this.isPlaying = true;
        }
    }

    private void stopPlayingResponse() {
        if (isPlaying && this.getActivity() != null ) {
            this.fadePlaybackProgressBarTo(0, R.integer.anim_short);
            this.geoStoryFragmentListener.doStopGeoStoryPlay();
            this.buttonReplay.setImageDrawable(playDrawable);
            this.isPlaying = false;
        }
    }

    private void fadePlaybackProgressBarTo(float alpha, int animLengthResId) {
        playbackProgressBar.animate()
                .alpha(alpha)
                .setDuration(getResources().getInteger(animLengthResId))
                .setListener(null);
    }

    /**
     * Called when the respond button (with the mic icon) is pressed.
     */
    public void onRespondButtonPressed() {
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
        this.textViewRespond.setText(getString(R.string.reflection_label_record));

        this.geoStoryFragmentListener.doStartGeoStoryRecording(this.promptParentId, this.promptId);
    }

    private void stopResponding() {
        this.geoStoryFragmentListener.doStopGeoStoryRecording();

        this.isResponding = false;
        this.fadeRecordingProgressBarTo(0, R.integer.anim_fast);
        this.textViewRespond.setText(getString(R.string.reflection_label_answer));
        this.doGoToPlaybackControl();
    }

    private void doGoToPlaybackControl() {
        this.mainViewAnimator.setInAnimation(getContext(), R.anim.view_move_left_next);
        this.mainViewAnimator.setOutAnimation(getContext(), R.anim.view_move_left_current);
        this.mainViewAnimator.showNext();
    }

    private void fadeRecordingProgressBarTo(float alpha, int animLengthResId) {
        recordingProgressBar.animate()
                .alpha(alpha)
                .setDuration(getResources().getInteger(animLengthResId))
                .setListener(null);
    }

    private boolean isRecordingAllowed() {
        int permissionRecordAudio = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO);
        return permissionRecordAudio == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Called when the Delete button is pressed.
     * @param context
     */
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

    /**
     * Called when the used pressed the Share button. This will immediately share the story.
     */
    private void onShareButtonPressed() {
        this.geoStoryFragmentListener.doShareGeoStory(geoLocation, geoStoryMeta);
        this.onGoToFragmentCallback.onGoToFragment(TransitionType.ZOOM_OUT, 1);
    }

    /**
     * Called when the user pressed the Edit Info button. This will evoke the dialog to edit the
     * metadata info of the story.
     */
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

}
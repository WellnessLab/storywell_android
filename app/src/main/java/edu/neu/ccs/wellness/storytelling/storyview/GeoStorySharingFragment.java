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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryMeta;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.people.PersonDoesNotExistException;
import edu.neu.ccs.wellness.story.GeoStorySharing;
import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.Storywell;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.utils.NearbyPlacesManagerInterface;
import edu.neu.ccs.wellness.storytelling.utils.OnGoToFragmentListener;
import edu.neu.ccs.wellness.storytelling.utils.PlaceItem;
import edu.neu.ccs.wellness.storytelling.utils.PlacesByZipCodes;
import edu.neu.ccs.wellness.storytelling.utils.StoryContentAdapter;
import edu.neu.ccs.wellness.utils.WellnessDate;

import static edu.neu.ccs.wellness.people.Person.ROLE_PARENT;


public class GeoStorySharingFragment extends Fragment implements
        View.OnClickListener,
        EditGeoStoryMetaDialogFragment.GeoStoryMetaListener,
        EditLocationDialogFragment.GeoStoryLocationListener,
        NearbyPlacesManagerInterface,
        OnMapReadyCallback {

    /* CONSTANTS */
    private static final Boolean DEFAULT_IS_RESPONSE_STATE = false;
    private static final int REQUEST_AUDIO_PERMISSIONS = 100;
    private static final int CHILD_RECORDING_SCREEN = 1;
    private static final int CHILD_PREVIEW_SCREEN = 2;
    private static final int CHILD_CONFIRMATION_SCREEN = 3;
    private static String[] permission = {Manifest.permission.RECORD_AUDIO}; // For audio permission

    /* FIELDS */
    private Storywell storywell;
    private OnGoToFragmentListener onGoToFragmentCallback;
    private GeoStoryFragmentListener geoStoryFragmentListener;
    private FusedLocationProviderClient fusedLocationClient;
    private NearbyPlacesManagerInterface nearbyPlacesManager;

    private View view;
    private ViewAnimator mainViewAnimator;

    private View startScreen;
    private ImageButton buttonReplay;
    private ImageButton buttonRespond;
    private Button buttonEdit;
    private Button buttonChangeLocation;
    private Button buttonDelete;
    private Button buttonShare;
    private Button buttonNext;
    private ProgressBar recordingProgressBar;
    private ProgressBar playbackProgressBar;
    private TextView textViewRespond;
    private TextView textViewName;
    private TextView textViewAvgSteps;
    private TextView textViewNeighborhood;
    private TextView textViewBio;
    private ImageView storyIconImageView;
    private TextView textViewPostedTime;
    private TextView textViewInstruction;

    private Drawable playDrawable;
    private Drawable stopDrawable;

    private GoogleMap storyGoogleMap;
    private Marker geoLocationMarker;

    private Location geostoryLocation;
    private GeoStory savedGeoStory;
    private GeoStoryMeta geoStoryMeta;
    private Address geoStoryAddress = new Address(Locale.US);
    private String promptParentId;
    private String promptId;

    private boolean isResponding = false;
    private boolean isResponseExists;
    private boolean isPlaying = false;
    private boolean isShowSavedGeoStory;
    private int highestIconLevel = 2;
    private SynchronizedSetting synchronizedSetting;

    /**
     * Listener that must be implemented by the {@link Activity} that uses this Fragment.
     */
    public interface GeoStoryFragmentListener {
        boolean isGeoStoryExists(String promptId);
        GeoStory getSavedGeoStory(String promptId);
        void doStartGeoStoryRecording(String promptParentId, String promptId);
        void doStopGeoStoryRecording();
        void doStartGeoStoryPlay(String promptId, OnCompletionListener completionListener);
        void doStopGeoStoryPlay();
        boolean doShareGeoStory(Location location, GeoStoryMeta geoStoryMeta);
        FusedLocationProviderClient getLocationProvider();
    }

    /* CONSTRUCTORS */
    public GeoStorySharingFragment() {
        // DO NOTHING
    }

    /* METHODS */
    /**
     * When the Fragment is created but before being attached to the Activity.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.storywell = new Storywell(this.getContext());

        this.synchronizedSetting = this.storywell.getSynchronizedSetting();
        this.highestIconLevel = synchronizedSetting.getFamilyInfo().getHighestIconLevel();
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

        this.playDrawable = getResources().getDrawable(R.drawable.ic_round_play_arrow_big);
        this.stopDrawable = getResources().getDrawable(R.drawable.ic_round_stop_big);

        this.view = inflater.inflate(R.layout.fragment_geostory_share, container, false);
        this.mainViewAnimator = getMainViewAnim(this.view);
        this.startScreen = view.findViewById(R.id.start_screen);
        this.buttonRespond = view.findViewById(R.id.button_respond);
        this.buttonReplay = view.findViewById(R.id.button_play);
        this.buttonEdit = view.findViewById(R.id.button_edit);
        this.buttonChangeLocation = view.findViewById(R.id.button_change_location);
        this.buttonDelete = view.findViewById(R.id.button_back);
        this.buttonShare = view.findViewById(R.id.button_share);
        this.buttonNext = view.findViewById(R.id.button_next);
        this.textViewRespond = view.findViewById(R.id.text_respond);
        this.textViewName = view.findViewById(R.id.caregiver_nickname);
        this.textViewAvgSteps = view.findViewById(R.id.average_steps);
        this.textViewNeighborhood = view.findViewById(R.id.neighborhood_info);
        this.textViewBio = view.findViewById(R.id.user_bio);
        this.textViewPostedTime = this.view.findViewById(R.id.posted_time);
        this.recordingProgressBar = view.findViewById(R.id.recording_progress_bar);
        this.playbackProgressBar = view.findViewById(R.id.playback_progress_bar);
        this.storyIconImageView = view.findViewById(R.id.caregiver_avatar);
        this.textViewInstruction = view.findViewById(R.id.geostory_instruction);

        this.view.findViewById(R.id.similarity_text).setVisibility(View.GONE);

        this.storyIconImageView.setImageResource(GeoStoryMapPresenter.getIconRes(highestIconLevel));

        this.buttonChangeLocation.setVisibility(View.VISIBLE);

        // Get the text to display from bundle and show it as view
        String text = getArguments().getString(StoryContentAdapter.KEY_TEXT);
        String subtext = getArguments().getString(StoryContentAdapter.KEY_SUBTEXT);
        setContentText(view, text, subtext);

        String instructionText = getString(R.string.geostory_instruction_text, getCaregiverName());
        this.textViewInstruction.setText(instructionText);

        this.view.findViewById(R.id.response_control).setVisibility(View.GONE);

        this.startScreen.setOnClickListener(this);
        this.buttonRespond.setOnClickListener(this);
        this.buttonReplay.setOnClickListener(this);
        this.buttonEdit.setOnClickListener(this);
        this.buttonDelete.setOnClickListener(this);
        this.buttonShare.setOnClickListener(this);
        this.buttonNext.setOnClickListener(this);
        this.buttonChangeLocation.setOnClickListener(this);
        view.findViewById(R.id.storymap_viewer_sheet).setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return view;
    }

    private String getCaregiverName() {
        String caregiverName = getString(R.string.caregiver_default_name);
        try {
            caregiverName = this.synchronizedSetting.getGroup()
                    .getPersonByRole(ROLE_PARENT).getName();
        } catch (PersonDoesNotExistException e) {
            e.printStackTrace();
        }
        return caregiverName;
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
            case R.id.start_screen:
                doGoToStartRecordingScreen();
                break;
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
            case R.id.button_change_location:
                onButtonChangeLocationPressed();
                break;
            case R.id.button_back:
                onButtonBackPressed(getContext());
                break;
            case R.id.button_next:
                onGoToFragmentCallback.onGoToFragment(OnGoToFragmentListener.TransitionType.SLIDE_LEFT, 1);
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

        try {
            this.nearbyPlacesManager = (NearbyPlacesManagerInterface) context;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassCastException(((Activity) context).getLocalClassName()
                    + " must implement NearbyPlacesManagerInterface");
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
            this.isShowSavedGeoStory = true;

            this.savedGeoStory = geoStoryFragmentListener.getSavedGeoStory(promptId);
            this.geoStoryMeta = this.savedGeoStory.getMeta();

            this.doGoToPlaybackScreen();
        } else {
            this.isShowSavedGeoStory = false;

            this.geoStoryMeta = this.getDefaultGeoStoryMeta();

            this.fetchCaregiverAverageSteps();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void doGoToStartRecordingScreen() {
        this.mainViewAnimator.setDisplayedChild(CHILD_RECORDING_SCREEN);
    }

    private void doGoToRedoRecordingScreen() {
        this.geoStoryFragmentListener.doStopGeoStoryPlay();
        this.mainViewAnimator.setDisplayedChild(CHILD_RECORDING_SCREEN);
    }

    private void doGoToPlaybackScreen() {
        // Update the StoryViewer with the saved GeoStory
        this.updateStoryViewer(savedGeoStory.getUserNickname(), savedGeoStory.getSteps(),
                geoStoryMeta.getNeighborhood(), geoStoryMeta.getBio(),
                savedGeoStory.getRelativeDate(), savedGeoStory.getMeta().getIconId());

        // Change the visibilities of the control buttons
        this.view.findViewById(R.id.sharing_control).setVisibility(View.GONE);
        this.view.findViewById(R.id.button_change_location).setVisibility(View.GONE);
        this.view.findViewById(R.id.button_share).setVisibility(View.GONE);
        this.view.findViewById(R.id.button_next).setVisibility(View.VISIBLE);

        // Show the story viewer for playback
        this.mainViewAnimator.setDisplayedChild(CHILD_PREVIEW_SCREEN);
    }

    private void doGoToPreviewControl() {
        // Update the StoryViewer with the saved GeoStory
        String nickname = this.geoStoryMeta.getUserNickname();
        String bio = this.geoStoryMeta.getBio();
        String relativeDate = getString(R.string.geostory_posted_time_default);
        this.updateStoryViewer(nickname, 1000, null, bio,
                relativeDate, highestIconLevel);

        // Change the visibilities of the control buttons
        this.view.findViewById(R.id.sharing_control).setVisibility(View.VISIBLE);
        this.view.findViewById(R.id.button_share).setVisibility(View.VISIBLE);
        this.view.findViewById(R.id.button_next).setVisibility(View.GONE);
        this.view.findViewById(R.id.button_change_location).setVisibility(View.VISIBLE);

        // Show the story viewer for preview
        this.mainViewAnimator.setDisplayedChild(CHILD_PREVIEW_SCREEN);
    }

    private void updateStoryViewer(String nickname, Integer averageSteps, String neighborhood,
                                   String bio, String relativeDate, Integer iconId) {
        if (nickname != null) {
            this.textViewName.setText(nickname);
        }

        if (averageSteps != null) {
            this.textViewAvgSteps.setText(String.valueOf(averageSteps));
        }

        if (neighborhood != null) {
            this.textViewNeighborhood.setText(neighborhood);
        }

        if (bio != null) {
            this.textViewBio.setText(bio);
        }

        if (relativeDate != null) {
            this.textViewPostedTime.setText(relativeDate);
        }

        if (iconId != null) {
            this.storyIconImageView.setImageResource(GeoStoryMapPresenter.getIconRes(iconId));

            if (this.geoLocationMarker != null) {
                this.geoLocationMarker.setIcon(GeoStoryMapPresenter.getStoryIcon(iconId));
            }
        }
    }

    private void doGoToConfirmationScreen() {
        this.mainViewAnimator.setDisplayedChild(CHILD_CONFIRMATION_SCREEN);
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
        this.storyGoogleMap.getUiSettings().setRotateGesturesEnabled(false);
        this.storyGoogleMap.getUiSettings().setTiltGesturesEnabled(false);

        // Update to home location
        LatLng homeLatLng = new LatLng(
                this.synchronizedSetting.getFamilyInfo().getHomeLatitude(),
                this.synchronizedSetting.getFamilyInfo().getHomeLongitude());
        CameraUpdate homeCameraUpdate = CameraUpdateFactory.newLatLng(homeLatLng);
        this.storyGoogleMap.moveCamera(homeCameraUpdate);
        // this.showMyLocationMarker();
        this.setLocationListener(this.geoStoryFragmentListener.getLocationProvider());
    }

    private void setLocationListener(FusedLocationProviderClient locationProvider) {
        this.fusedLocationClient = locationProvider;
        if (ActivityCompat.checkSelfPermission(
                this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
            if (location == null) {
                return;
            }

            final int iconId;

            if (isShowSavedGeoStory) {
                geostoryLocation = savedGeoStory.getLocation();
                iconId = geoStoryMeta.getIconId();
            } else {
                geoStoryMeta.setOriginalLatitude(location.getLatitude());
                geoStoryMeta.setOriginalLongitude(location.getLongitude());
                geostoryLocation = GeoStoryMapPresenter.getOffsetLocation(location);
                fetchAddress(geostoryLocation);
                iconId = highestIconLevel;
            }

            storyGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    addLocationMarker(geostoryLocation, iconId);
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void showMyLocationMarker() {
        if (GeoStoryMapPresenter.isAccessLocationGranted(getContext())) {
            storyGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void addLocationMarker(Location location, int iconId) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = GeoStoryMapPresenter.getSharingLocationMarker(latLng);
        this.geoLocationMarker = this.storyGoogleMap.addMarker(markerOptions);
        this.geoLocationMarker.setIcon(GeoStoryMapPresenter.getStoryIcon(iconId));

        CameraUpdate initialPos = CameraUpdateFactory.newLatLngZoom(latLng, 16);
        this.storyGoogleMap.animateCamera(initialPos);
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
            Location location = mFragment.getGeostoryLocation();
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
        PlacesByZipCodes placesByZipCodes = new PlacesByZipCodes();
        String zipcode = address.getPostalCode();
        String neighborhood = placesByZipCodes.getNeighborhoodByZipCode(
                zipcode, address.getLocality());

        this.geoStoryAddress = address;
        // this.geoStoryMeta.setNeighborhood(address.getLocality());
        // this.textViewNeighborhood.setText(this.geoStoryAddress.getLocality());
        this.geoStoryMeta.setNeighborhood(neighborhood);
        this.textViewNeighborhood.setText(neighborhood);
    }

    protected Location getGeostoryLocation() {
        return this.geostoryLocation;
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
        this.geoStoryMeta.setIconId(geoStoryMeta.getIconId());

        this.setAverageStepsVisibility(geoStoryMeta.isShowAverageSteps());
        this.setNeighborhoodInfoVisibility(geoStoryMeta.isShowNeighborhood());
        /*
        this.textViewBio.setText(geoStoryMeta.getBio());
        this.geoLocationMarker.setIcon(GeoStoryMapPresenter.getStoryIcon(geoStoryMeta.getIconId()));
        this.storyIconImageView.setImageResource(
                GeoStoryMapPresenter.getIconRes(geoStoryMeta.getIconId()));
        */
        this.updateStoryViewer(null, null, null,
                geoStoryMeta.getBio(),
                null, geoStoryMeta.getIconId());
    }

    private void setAverageStepsVisibility(boolean isShow) {
        if (isShow) {
            this.view.findViewById(R.id.steps_info).setVisibility(View.VISIBLE);
        } else {
            this.view.findViewById(R.id.steps_info).setVisibility(View.GONE);
        }
    }

    private void setNeighborhoodInfoVisibility(boolean isShow) {
        if (isShow) {
            this.textViewNeighborhood.setVisibility(View.VISIBLE);
            this.view.findViewById(R.id.dash1_label).setVisibility(View.VISIBLE);
        } else {
            this.textViewNeighborhood.setVisibility(View.GONE);
            this.view.findViewById(R.id.dash1_label).setVisibility(View.GONE);
        }
    }

    /**
     * Set
     * @param placeName
     * @param lat
     * @param lng
     */
    @Override
    public void setLocationEdit(String placeName, Double lat, Double lng) {
        Location location = GeoStoryMapPresenter.getLocationFromLatLng(lat, lng);

        this.geoStoryMeta.setNeighborhood(placeName);
        this.textViewNeighborhood.setText(placeName);
        this.geostoryLocation = location;
        this.geoLocationMarker.setPosition(new LatLng(lat, lng));

        CameraUpdate targetPos = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16);
        this.storyGoogleMap.animateCamera(targetPos);
    }

    @Override
    public void setPlaceItemList(List<PlaceItem> placeItemList) {
        // this.placeItemList = placeItemList;
        this.nearbyPlacesManager.setPlaceItemList(placeItemList);
    }

    @Override
    public List<PlaceItem> getPlaceItemList() {
        // return this.placeItemList;
        return this.nearbyPlacesManager.getPlaceItemList();
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
        this.doGoToPreviewControl();
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
                doGoToRedoRecordingScreen();
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

    /**
     * Called when the used pressed the Share button. This will immediately share the story.
     */
    private void onShareButtonPressed() {
        this.geoStoryFragmentListener.doShareGeoStory(geostoryLocation, geoStoryMeta);
        //this.onGoToFragmentCallback.onGoToFragment(TransitionType.ZOOM_OUT, 1);
        this.doGoToConfirmationScreen();
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
        DialogFragment newFragment = EditGeoStoryMetaDialogFragment.newInstance(
                geoStoryMeta, highestIconLevel);
        newFragment.setTargetFragment(GeoStorySharingFragment.this, 300);
        newFragment.show(ft, EditGeoStoryMetaDialogFragment.TAG);
    }


    /**
     * Called when the user pressed the Change Location button. This will evoke the dialog to edit
     * the location of the story.
     */
    private void onButtonChangeLocationPressed() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(EditLocationDialogFragment.TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        String neighborhood = this.geoStoryAddress.getLocality();
        DialogFragment newFragment = EditLocationDialogFragment.newInstance(neighborhood,
                geostoryLocation.getLatitude(), geostoryLocation.getLongitude());
        newFragment.setTargetFragment(GeoStorySharingFragment.this, 300);
        newFragment.show(ft, EditLocationDialogFragment.TAG);
    }

    public GeoStoryMeta getDefaultGeoStoryMeta () {
        GeoStoryMeta meta = new GeoStoryMeta();
        meta.setPromptParentId(this.promptParentId);
        meta.setPromptId(this.promptId);
        meta.setUserNickname(this.synchronizedSetting.getFamilyInfo().getCaregiverNickname());
        meta.setBio(this.synchronizedSetting.getFamilyInfo().getCaregiverBio());
        meta.setIconId(highestIconLevel);

        return meta;
    }

}
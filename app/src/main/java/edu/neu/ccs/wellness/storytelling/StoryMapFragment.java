package edu.neu.ccs.wellness.storytelling;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.neu.ccs.wellness.fitness.MultiDayFitness;
import edu.neu.ccs.wellness.fitness.storage.FitnessRepository;
import edu.neu.ccs.wellness.geostory.FirebaseUserGeoStoryMetaRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.viewmodel.StoryMapViewModel;
import edu.neu.ccs.wellness.utils.WellnessDate;

public class StoryMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    /* CONSTANTS */
    private static final String VIEW_LAT = "VIEW_LAT";
    private static final String VIEW_LONG = "VIEW_LONG";
    private static final int AVG_STEPS_UNSET = -1;
    private static final String KEY_CAMERA_STATE = "KEY_CAMERA_STATE";

    /* FIELDS */
    private ConstraintLayout storyMapViewerSheet;
    private GoogleMap storyGoogleMap;
    private CameraUpdate initialCameraPos;

    private GroundOverlay markerHighlightOverlay;
    private ValueAnimator markerHighlightInAnim;

    private StoryMapLiveData storyMapLiveData;
    private LiveData<UserGeoStoryMeta> userStoryMapMetaLiveData;
    private Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
    private Set<String> addedStorySet = new HashSet<>();
    private UserGeoStoryMeta userGeoStoryMeta;
    private String currentGeoStoryName = "";
    private int caregiverAvgSteps = AVG_STEPS_UNSET;
    private int globalMinSteps = 0;
    private int globalMaxSteps = 0;

    private Person caregiver;
    private LatLng homeLatLng;

    private GeoStory currentGeoStory;
    private boolean isShowingNewGeoStory = false;
    private boolean isPlayingStory;

    private BottomSheetBehavior geoStorySheetBehavior;

    private View geoStoryOverview;
    private TextView postedTimeView;
    private TextView nicknameView;
    private TextView avgStepsView;
    private TextView neighborhoodView;
    private TextView bioView;
    private ImageButton buttonPlay;
    private ImageView imageAvatar;
    private ProgressBar progressBarPlay;
    private MediaPlayer mediaPlayer;



    private Map<String, Float> geoStoryMatchMap = new HashMap<>();
    private FusedLocationProviderClient locationProvider;
    private FirebaseUserGeoStoryMetaRepository userResponseRepository;

    /* CONSTRUCTOR */
    public StoryMapFragment() {
        markerHighlightInAnim = ValueAnimator.ofFloat(0, 1.0f, 0);
        markerHighlightInAnim.setStartDelay(150);
        markerHighlightInAnim.setDuration(850);
        markerHighlightInAnim.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    /* FACTORY METHOD */
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param latitude Caregiver's home latitude.
     * @param longitude Caregiver's home longitude.
     * @return A new instance of fragment StoryMapFragment.
     */
    public static StoryMapFragment newInstance(double latitude, double longitude) {
        StoryMapFragment fragment = new StoryMapFragment();
        Bundle args = new Bundle();
        args.putDouble(VIEW_LAT, latitude);
        args.putDouble(VIEW_LONG, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    public static StoryMapFragment newInstance() {
        return new StoryMapFragment();
    }

    /* INTERFACE METHODS */
    /**
     * Called when Fragment created.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StoryMapViewModel viewModel = ViewModelProviders.of(this)
                .get(StoryMapViewModel.class);

        this.storyMapLiveData = (StoryMapLiveData) viewModel.getStoryMapLiveData();
        this.userStoryMapMetaLiveData = viewModel.getUserStoryMetaLiveData(this.getContext());

        if (savedInstanceState != null) {
            initialCameraPos = CameraUpdateFactory.newCameraPosition(
                    (CameraPosition) savedInstanceState.getParcelable(KEY_CAMERA_STATE));
        }
    }

    /**
     * Called when View for the Fragment created.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_storymap, container, false);

        /* Prepare the bottom sheet to view stories */
        this.storyMapViewerSheet = rootView.findViewById(R.id.storymap_viewer_sheet);

        this.imageAvatar = this.storyMapViewerSheet.findViewById(R.id.caregiver_avatar);
        this.nicknameView = this.storyMapViewerSheet.findViewById(R.id.caregiver_nickname);
        this.avgStepsView = this.storyMapViewerSheet.findViewById(R.id.average_steps);
        this.postedTimeView = this.storyMapViewerSheet.findViewById(R.id.posted_time);
        this.neighborhoodView = this.storyMapViewerSheet.findViewById(R.id.neighborhood_info);
        this.bioView = this.storyMapViewerSheet.findViewById(R.id.user_bio);
        this.buttonPlay = this.storyMapViewerSheet.findViewById(R.id.button_play);
        this.progressBarPlay = this.storyMapViewerSheet.findViewById(R.id.playback_progress_bar);
        this.geoStoryOverview = this.storyMapViewerSheet.findViewById(R.id.overview);

        /* PREPARE THE STORY SHEET */
        this.geoStorySheetBehavior = BottomSheetBehavior.from(storyMapViewerSheet);
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        this.geoStorySheetBehavior.setHideable(true);

        // Set callback for changes
        this.geoStorySheetBehavior.setBottomSheetCallback(geoStorySheetBottomSheetCallback);

        // Prepare the Bottom Sheet Behavior
        this.geoStoryOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleStorySheet();
            }
        });

        this.buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCurrentGeoStory();
            }
        });

        return rootView;
    }

    private void toggleStorySheet() {
        switch (geoStorySheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_COLLAPSED:
                this.currentGeoStoryName = "";
                geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            case BottomSheetBehavior.STATE_EXPANDED:
                geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            default:
                break;
        }
    }

    /**
     * Called when the activity was created.
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (getActivity() != null) {
            Storywell storywell = new Storywell(getContext());
            SynchronizedSetting synchronizedSetting = storywell.getSynchronizedSetting();
            this.locationProvider = LocationServices.getFusedLocationProviderClient(getActivity());

            this.userResponseRepository = new FirebaseUserGeoStoryMetaRepository(
                    storywell.getGroup().getName());

            this.caregiver = storywell.getCaregiver();
            this.homeLatLng = new LatLng(
                    synchronizedSetting.getFamilyInfo().getHomeLatitude(),
                    synchronizedSetting.getFamilyInfo().getHomeLongitude());

            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.map);

            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
    }


    /**
     * Called when the fragment is paused.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (storyGoogleMap != null) {
            outState.putParcelable(KEY_CAMERA_STATE, storyGoogleMap.getCameraPosition());
        }
    }

    /**
     * Called when the {@link GoogleMap} is ready.
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.storyGoogleMap = googleMap;
        this.storyGoogleMap.setOnMarkerClickListener(this);
        this.storyGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        this.storyGoogleMap.getUiSettings().setRotateGesturesEnabled(false);
        this.storyGoogleMap.getUiSettings().setTiltGesturesEnabled(false);
        showMyLocationMarker();
        addHomeMarker();
        fetchUserGeoStoryMeta();

        this.initCenterMap(homeLatLng);
        this.setLocationListener(locationProvider);


        GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory
                        .fromResource(R.mipmap.art_geostory_marker_highlight_glow))
                .position(homeLatLng, 500)
                .transparency(1);
        markerHighlightOverlay = storyGoogleMap.addGroundOverlay(groundOverlayOptions);
    }

    @SuppressLint("MissingPermission")
    private void setLocationListener(FusedLocationProviderClient locationProvider) {
        if (StoryMapPresenter.isAccessLocationGranted(getContext())) {
            locationProvider.getLastLocation().addOnSuccessListener(
                    this.getActivity(), locationListener);
        }
    }

    /**
     * Listener to handle the GPS location fetching.
     */
    private OnSuccessListener<Location> locationListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(final Location location) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (initialCameraPos == null) {
                initialCameraPos = CameraUpdateFactory.newLatLngZoom(latLng, 12);
            }

            storyGoogleMap.moveCamera(initialCameraPos);
        }
    };

    @SuppressLint("MissingPermission")
    private void initCenterMap(LatLng defaultLatLng) {
        if (StoryMapPresenter.isAccessLocationGranted(getContext())) {
            LatLng latLng;
            LocationManager locationManager = (LocationManager) getActivity()
                    .getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, false);

            if (bestProvider != null) {
                Location location = locationManager.getLastKnownLocation(bestProvider);
                latLng = new LatLng(location.getLatitude(), location.getLongitude());
            } else {
                latLng = defaultLatLng;
            }

            this.storyGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
        }
    }

    /**
     * Fetch the current user's geostory meta information.
     */
    private void fetchUserGeoStoryMeta() {
        this.userStoryMapMetaLiveData.observe(this, new Observer<UserGeoStoryMeta>() {
            @Override
            public void onChanged(@Nullable UserGeoStoryMeta dataSnapshot) {
                userGeoStoryMeta = dataSnapshot;
                fetchGeoStoryMap();
            }
        });
    }

    /**
     * Fetch the list of GeoStoryMap
     */
    private void fetchGeoStoryMap() {
        this.storyMapLiveData.observe(this, new Observer<Map<String, GeoStory>>() {
            @Override
            public void onChanged(@Nullable Map<String, GeoStory> dataSnapshot) {
                geoStoryMap = dataSnapshot;
                globalMinSteps = storyMapLiveData.getMinSteps();
                globalMaxSteps = storyMapLiveData.getMaxSteps();

                fetchAverageStepsThenPrepareMap();
            }
        });
    }

    /**
     * Fetch caregivers average steps, then start prepare map.
     */
    private void fetchAverageStepsThenPrepareMap() {
        Calendar startCal = WellnessDate.getBeginningOfDay();
        Calendar endCal = WellnessDate.getBeginningOfDay();

        startCal.add(Calendar.DATE, -8);
        endCal.add(Calendar.DATE, -1);

        final Date startDate = startCal.getTime();
        final Date endDate = endCal.getTime();

        FitnessRepository fitnessRepository = new FitnessRepository();
        fitnessRepository.fetchDailyFitness(caregiver, startDate, endDate, new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                MultiDayFitness multiDayFitness = FitnessRepository
                        .getMultiDayFitness(startDate, endDate, dataSnapshot);
                caregiverAvgSteps = multiDayFitness.getStepsAverage();
                prepareMap();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void prepareMap() {
        populateMap();
    }

    @SuppressLint("MissingPermission")
    private void showMyLocationMarker() {
        if (StoryMapPresenter.isAccessLocationGranted(getContext())) {
            storyGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void addHomeMarker() {
        Marker homeMarker = storyGoogleMap.addMarker(StoryMapPresenter.getHomeMarker(homeLatLng));
        homeMarker.setTag(StoryMapPresenter.TAG_HOME);
    }

    private void populateMap() {
        for (Map.Entry<String, GeoStory> entry : this.geoStoryMap.entrySet()) {
            String geoStoryName = entry.getKey();
            if (!this.addedStorySet.contains(geoStoryName)) {
                GeoStory geoStory = entry.getValue();
                if (geoStory.isReviewed()) {
                    Marker marker = storyGoogleMap.addMarker(getMarker(geoStory, geoStoryName));
                    marker.setTag(geoStoryName);
                }
            }
        }
    }

    private MarkerOptions getMarker(GeoStory geoStory, String geoStoryName) {
        float match = geoStory.getFitnessRatio(caregiverAvgSteps, globalMinSteps, globalMaxSteps);
        this.addedStorySet.add(geoStoryName);
        this.geoStoryMatchMap.put(geoStoryName, match);
        if (userGeoStoryMeta.isStoryRead(geoStoryName)) {
            return StoryMapPresenter.getMarkerOptions(geoStory, match, true);
        } else {
            return StoryMapPresenter.getMarkerOptionsById(geoStory, geoStory.getMeta().getIconId());
        }
    }

    /** Called when the user clicks a marker.
     * @param marker
     * @return Return false to indicate that we have not consumed the event and that we wish for
     * the default behavior to occur (which is for the camera to move such that the marker is
     * centered and for the marker's info window to open, if it has one).
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        String geoStoryName = (String) marker.getTag();
        if (!StoryMapPresenter.TAG_HOME.equals(geoStoryName)) {
            showGeoStory(geoStoryName, marker);
            this.userResponseRepository.addStoryAsRead(geoStoryName);
        }
        return false;
    }

    private void showGeoStory(String geoStoryName, Marker marker) {
        switch (this.geoStorySheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_HIDDEN:
                updateStorySheet(geoStoryName);
                showCollapsedStorySheet(geoStoryName);
                showMarkerHighlight(geoStoryName, marker);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
            case BottomSheetBehavior.STATE_EXPANDED:
                if (currentGeoStoryName.equals(geoStoryName)) {
                    hideGeoStory();
                } else {
                    hideAndShowStorySheet(geoStoryName);
                    showMarkerHighlight(geoStoryName, marker);
                }
                break;
            default:
                break;
        }
    }

    private void showMarkerHighlight(final String geoStoryName, final Marker marker) {
        if (!userGeoStoryMeta.isStoryRead(geoStoryName)) {
            markerHighlightOverlay.setTransparency(0);
            markerHighlightOverlay.setDimensions(0);
            markerHighlightOverlay.setPosition(marker.getPosition());

            markerHighlightInAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                boolean isMarkerRevealed = false;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float ratio = (float) animation.getAnimatedValue();
                    markerHighlightOverlay.setTransparency(1 - ratio);

                    if (ratio < 1.0 && !isMarkerRevealed) {
                        markerHighlightOverlay.setDimensions(1000 * ratio);
                    }

                    if (ratio >= 0.95 && !isMarkerRevealed) {
                        isMarkerRevealed = true;
                        marker.setIcon(StoryMapPresenter.getIconByMatchValue(
                                geoStoryMatchMap.get(geoStoryName), true));
                    }
                }
            });
            markerHighlightInAnim.start();
        }
    }

    private void updateStorySheet(String geoStoryName) {
        float match = geoStoryMatchMap.get(geoStoryName);
        currentGeoStory = this.geoStoryMap.get(geoStoryName);
        nicknameView.setText(currentGeoStory.getUserNickname());
        postedTimeView.setText(currentGeoStory.getRelativeDate());
        bioView.setText(currentGeoStory.getBio());
        imageAvatar.setImageResource(StoryMapPresenter.getBitmapResource(match));

        if (currentGeoStory.getMeta().isShowAverageSteps()) {
            avgStepsView.setText(String.valueOf(currentGeoStory.getSteps()));
            storyMapViewerSheet.findViewById(R.id.steps_info).setVisibility(View.VISIBLE);
        } else {
            storyMapViewerSheet.findViewById(R.id.steps_info).setVisibility(View.GONE);
        }

        if (currentGeoStory.getMeta().isShowNeighborhood()) {
            storyMapViewerSheet.findViewById(R.id.neighborhood_info).setVisibility(View.VISIBLE);
        } else {
            neighborhoodView.setText(currentGeoStory.getNeighborhood());
            storyMapViewerSheet.findViewById(R.id.neighborhood_info).setVisibility(View.GONE);
        }
    }

    private void hideAndShowStorySheet(String geoStoryName) {
        this.isShowingNewGeoStory = true;
        this.currentGeoStoryName = geoStoryName;
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void showCollapsedStorySheet(String geoStoryName) {
        this.currentGeoStoryName = geoStoryName;
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideGeoStory() {
        this.currentGeoStoryName = "";
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * GeoStory's {@link BottomSheetBehavior.BottomSheetCallback}.
     */
    private BottomSheetBehavior.BottomSheetCallback geoStorySheetBottomSheetCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_HIDDEN:
                            stopPlayingResponse();
                            if (isShowingNewGeoStory) {
                                updateStorySheet(currentGeoStoryName);
                                isShowingNewGeoStory = false;
                                geoStorySheetBehavior.setState(
                                        BottomSheetBehavior.STATE_COLLAPSED);
                            }
                            break;
                        default:
                            // Don't do anything
                            break;
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                }
            };

    /**
     * Play current {@link GeoStory}.
     */
    private void playCurrentGeoStory() {
        if (isPlayingStory) {
            this.stopPlayingResponse();
        } else {
            this.startPlayingResponse();
        }
    }

    private void startPlayingResponse() {
        if (!isPlayingStory) {
            this.fadePlaybackProgressBarTo(1, R.integer.anim_short);
            this.startPlayback(currentGeoStory.getStoryUri());
            this.buttonPlay.setImageResource(R.drawable.ic_round_stop_big);
            this.isPlayingStory = true;
        }
    }

    private void stopPlayingResponse() {
        if (isPlayingStory && this.getActivity() != null ) {
            this.fadePlaybackProgressBarTo(0, R.integer.anim_short);
            this.stopPlayback();
            this.buttonPlay.setImageResource(R.drawable.ic_round_play_arrow_big);
            this.isPlayingStory = false;
        }
    }

    private void fadePlaybackProgressBarTo(float alpha, int animLengthResId) {
        this.progressBarPlay.animate()
                .alpha(alpha)
                .setDuration(getResources().getInteger(animLengthResId))
                .setListener(null);
    }

    /* AUDIO PLAYBACK METHODS */
    private void startPlayback(String uri) {
        this.mediaPlayer = new MediaPlayer();
        try {
            this.mediaPlayer.setDataSource(uri);
            this.mediaPlayer.prepare();
            this.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlayingResponse();
            }
        });
    }

    public void stopPlayback() {
        if (this.mediaPlayer != null) {
            if (this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.stop();
            }
            this.mediaPlayer.release();
            this.mediaPlayer = null;
        }
    }


}

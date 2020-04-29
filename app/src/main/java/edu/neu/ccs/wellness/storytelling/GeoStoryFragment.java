package edu.neu.ccs.wellness.storytelling;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.ArrayMap;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import edu.neu.ccs.wellness.geostory.FirebaseGeoStoryRepository;
import edu.neu.ccs.wellness.geostory.FirebaseUserGeoStoryMetaRepository;
import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.GeoStoryResolutionStatus;
import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;
import edu.neu.ccs.wellness.people.Person;
import edu.neu.ccs.wellness.storytelling.homeview.ChallengeCompletedDialog;
import edu.neu.ccs.wellness.storytelling.homeview.CloseChallengeUnlockStoryAsync;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryMapLiveData;
import edu.neu.ccs.wellness.storytelling.homeview.GeoStoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.homeview.HomeAdventurePresenter;
import edu.neu.ccs.wellness.storytelling.homeview.ReactionsListDialog;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.storytelling.utils.UserLogging;
import edu.neu.ccs.wellness.storytelling.viewmodel.GeoStoryMapViewModel;
import edu.neu.ccs.wellness.utils.WellnessDate;
import edu.neu.ccs.wellness.utils.WellnessIO;

public class GeoStoryFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    /* CONSTANTS */
    private static final String VIEW_LAT = "VIEW_LAT";
    private static final String VIEW_LONG = "VIEW_LONG";
    private static final int AVG_STEPS_UNSET = -1;
    private static final String KEY_CAMERA_STATE = "KEY_CAMERA_STATE";
    public static final String KEY_SHOW_RESOLUTION_GEOSTORY = "KEY_SHOW_RESOLUTION_GEOSTORY";
    private static final String TRANSLATION_Y = "translationY";
    private static final int ZERO_DP = 0;

    /* FIELDS */
    private Storywell storywell;
    private SynchronizedSetting.ResolutionInfo resolutionInfo;
    private ConstraintLayout storyMapViewerSheet;
    private GoogleMap storyGoogleMap;
    private CameraUpdate initialCameraPos;

    private GroundOverlay markerHighlightOverlay;
    private ValueAnimator markerHighlightInAnim;

    private GeoStoryMapLiveData geoStoryMapLiveData;
    private LiveData<UserGeoStoryMeta> userStoryMapMetaLiveData;
    private Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
    private Set<String> addedStorySet = new HashSet<>();
    private UserGeoStoryMeta userGeoStoryMeta;
    private String currentGeoStoryName = "";
    private int caregiverAvgSteps = AVG_STEPS_UNSET;
    private int globalMinSteps = 0;
    private int globalMaxSteps = 0;
    private Map<String, Integer> userReactionsMap;
    private Map<String, Map<String, Integer>> geoStoryUserReactionsMap;

    private Person caregiver;
    private LatLng homeLatLng;

    private GeoStory currentGeoStory;
    private boolean isShowingNewGeoStory = false;
    private boolean isPlayingStory;

    private BottomSheetBehavior geoStorySheetBehavior;

    private View rootView;
    private View geoStoryOverview;
    private TextView postedTimeView;
    private TextView nicknameView;
    private TextView avgStepsView;
    private TextView similarityView;
    private TextView neighborhoodView;
    private TextView bioView;
    private ImageButton buttonPlay;
    private ImageView imageAvatar;
    private ProgressBar progressBarPlay;
    private MediaPlayer mediaPlayer;
    private TextView numOfReactionsText;
    private Button buttonLike;
    private PopupMenu reactionsMenu;
    private Snackbar geoStoryMapSnackbar;

    private View geoStoryTopInfoBar;
    private View geoStoryBottomInfoBar;
    private View geoStoryActionBar;

    private Map<String, Float> geoStoryMatchMap = new HashMap<>();
    private FusedLocationProviderClient locationProvider;
    private FirebaseUserGeoStoryMetaRepository userResponseRepository;
    private FirebaseGeoStoryRepository firebaseGeoStoryRepository;
    private float scaleDP;
    private String[] reactionEmotionNames;
    private String notifGeoStoryId;
    private String notifUserNickname;

    /* CONSTRUCTOR */
    public GeoStoryFragment() {
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
     * @param latitude  Caregiver's home latitude.
     * @param longitude Caregiver's home longitude.
     * @return A new instance of fragment GeoStoryFragment.
     */
    public static GeoStoryFragment newInstance(double latitude, double longitude) {
        GeoStoryFragment fragment = new GeoStoryFragment();
        Bundle args = new Bundle();
        args.putDouble(VIEW_LAT, latitude);
        args.putDouble(VIEW_LONG, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    public static GeoStoryFragment newInstance(Bundle incomingExtras) {
        GeoStoryFragment fragment = new GeoStoryFragment();

        if (incomingExtras != null) {
            fragment.notifUserNickname = incomingExtras.getString("geoStoryAuthorNickname");
            fragment.notifGeoStoryId = incomingExtras.getString("geoStoryId");
        }
        return fragment;
    }

    /* INTERFACE METHODS */

    /**
     * Called when Fragment created.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeoStoryMapViewModel viewModel = ViewModelProviders.of(this)
                .get(GeoStoryMapViewModel.class);

        this.storywell = new Storywell(getContext());

        this.geoStoryMapLiveData = (GeoStoryMapLiveData) viewModel.getGeoStoryMapLiveData();
        this.userStoryMapMetaLiveData = viewModel.getUserStoryMetaLiveData(this.getContext());

        if (savedInstanceState != null) {
            initialCameraPos = CameraUpdateFactory.newCameraPosition(
                    (CameraPosition) savedInstanceState.getParcelable(KEY_CAMERA_STATE));
        }
    }

    /**
     * Called when View for the Fragment created.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_geostory_map, container, false);

        /* Prepare basic variables */
        scaleDP = getContext().getResources().getDisplayMetrics().density;

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
        this.geoStoryOverview = this.storyMapViewerSheet.findViewById(R.id.overview_area);
        this.similarityView = storyMapViewerSheet.findViewById(R.id.similarity_text);
        this.numOfReactionsText = storyMapViewerSheet.findViewById(R.id.liked_text);
        this.buttonLike = storyMapViewerSheet.findViewById(R.id.button_respond_story);

        this.geoStoryTopInfoBar = rootView.findViewById(R.id.geostory_top_info_bar);
        this.geoStoryBottomInfoBar = rootView.findViewById(R.id.geostory_bottom_info_bar);
        this.geoStoryActionBar = rootView.findViewById(R.id.geostory_top_action_bar);

        ImageView heroIcon = geoStoryTopInfoBar.findViewById(R.id.geostory_top_info_icon);
        if (storywell.getSynchronizedSetting().getHeroCharacterId() == 0) {
            heroIcon.setImageResource(R.drawable.art_hero_mira_completed_full);
        } else {
            heroIcon.setImageResource(R.drawable.art_hero_diego_completed_full);
        }

        /* PREPARE THE STORY SHEET */
        this.geoStorySheetBehavior = BottomSheetBehavior.from(storyMapViewerSheet);
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

        this.initReactionsMenu(buttonLike);
        this.buttonLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReactionsMenu();
            }
        });
        this.numOfReactionsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReactionsListDialog(view);
            }
        });

        rootView.findViewById(R.id.button_unlock_story).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showUnlockStoryDialog(rootView);
                    }
                });

        if (notifUserNickname != null) {
            showFindUserSnackbar(rootView, notifGeoStoryId, notifUserNickname);
        }

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
     *
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

    @Override
    public void onStart() {
        super.onStart();

        if (currentGeoStory == null) {
            this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            updateStorySheet(currentGeoStory.getStoryId());
        }
    }

    /**
     * Called when the fragment is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        this.refreshResolutionInfo();
        this.tryShowResolutionInfoSnackbar();
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
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (this.storyGoogleMap == null) {
            this.storyGoogleMap = googleMap;
            this.storyGoogleMap.setOnMarkerClickListener(this);
            this.storyGoogleMap.getUiSettings().setMapToolbarEnabled(false);
            this.storyGoogleMap.getUiSettings().setRotateGesturesEnabled(false);
            this.storyGoogleMap.getUiSettings().setTiltGesturesEnabled(false);
            showMyLocationMarker();
            // addHomeMarker();
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
    }

    @SuppressLint("MissingPermission")
    private void setLocationListener(FusedLocationProviderClient locationProvider) {
        if (GeoStoryMapPresenter.isAccessLocationGranted(getContext())) {
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
            if (location == null) {
                return;
            }

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (initialCameraPos == null) {
                initialCameraPos = CameraUpdateFactory.newLatLngZoom(latLng, 12);
            }

            storyGoogleMap.moveCamera(initialCameraPos);
        }
    };

    @SuppressLint("MissingPermission")
    private void initCenterMap(LatLng defaultLatLng) {
        if (GeoStoryMapPresenter.isAccessLocationGranted(getContext())) {
            LatLng latLng;
            LocationManager locationManager = (LocationManager) getActivity()
                    .getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, false);

            if (bestProvider != null) {
                Location location = locationManager.getLastKnownLocation(bestProvider);
                latLng = getOptionalLatLng(location, defaultLatLng);
            } else {
                latLng = defaultLatLng;
            }

            this.storyGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
        }
    }

    private LatLng getOptionalLatLng(Location location, LatLng defaultLatLng) {
        if (location == null) {
            return defaultLatLng;
        } else {
            return new LatLng(location.getLatitude(), location.getLongitude());
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
        this.geoStoryMapLiveData.observe(this, new Observer<Map<String, GeoStory>>() {
            @Override
            public void onChanged(@Nullable Map<String, GeoStory> dataSnapshot) {
                geoStoryMap = dataSnapshot;
                globalMinSteps = geoStoryMapLiveData.getMinSteps();
                globalMaxSteps = geoStoryMapLiveData.getMaxSteps();

                if (userReactionsMap == null) {
                    userReactionsMap = geoStoryMapLiveData.getUserReactionsSet();
                }

                fetchAverageStepsThenPrepareMap();
                updateGeoStoryCurrentlyShown();
            }
        });
    }

    private void updateGeoStoryCurrentlyShown() {
        if (currentGeoStory != null) {
            currentGeoStory = geoStoryMap.get(currentGeoStory.getStoryId());
            currentGeoStoryName = currentGeoStory.getStoryId();
            updateStorySheet(currentGeoStoryName);
        }
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
        fitnessRepository.fetchDailyFitness(caregiver, startDate, endDate, new ValueEventListener() {
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
        if (GeoStoryMapPresenter.isAccessLocationGranted(getContext())) {
            storyGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void addHomeMarker() {
        Marker homeMarker = storyGoogleMap.addMarker(GeoStoryMapPresenter.getHomeMarker(homeLatLng));
        homeMarker.setTag(GeoStoryMapPresenter.TAG_HOME);
    }

    private void addHeroMarker(LatLng latLng) {
        int heroCharacterId = storywell.getSynchronizedSetting().getHeroCharacterId();
        Marker heroMarker = storyGoogleMap.addMarker(
                GeoStoryMapPresenter.getHeroMarker(latLng, heroCharacterId, getContext()));
        heroMarker.setTag(GeoStoryMapPresenter.TAG_HERO);
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
            return GeoStoryMapPresenter.getMarkerOptions(geoStory, match, true);
        } else {
            return GeoStoryMapPresenter.getMarkerOptionsById(geoStory, geoStory.getMeta().getIconId());
        }
    }

    /**
     * Called when the user clicks a marker.
     *
     * @param marker
     * @return Return false to indicate that we have not consumed the event and that we wish for
     * the default behavior to occur (which is for the camera to move such that the marker is
     * centered and for the marker's info window to open, if it has one).
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        String geoStoryName = (String) marker.getTag();

        if (isStoryMarker(marker)) {
            showGeoStory(geoStoryName, marker);
            this.userResponseRepository.addStoryAsRead(geoStoryName);
        }

        if (geoStoryMapSnackbar != null) {
            geoStoryMapSnackbar.dismiss();
            geoStoryMapSnackbar = null;
        }
        
        return false;
    }

    private boolean isStoryMarker(Marker marker) {
        if (marker.getTag() == null) {
            return false;
        } else if (GeoStoryMapPresenter.TAG_HOME.equals(marker.getTag())) {
            return false;
        } else if (GeoStoryMapPresenter.TAG_HERO.equals(marker.getTag())) {
            return false;
        } else {
            return true;
        }
    }

    private void showGeoStory(String geoStoryName, Marker marker) {
        switch (this.geoStorySheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_HIDDEN:
                updateStorySheet(geoStoryName);
                showCollapsedStorySheet(geoStoryName);
                showMarkerHighlight(geoStoryName, marker);
                UserLogging.logViewGeoStory(geoStoryName);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
            case BottomSheetBehavior.STATE_EXPANDED:
                if (currentGeoStoryName.equals(geoStoryName)) {
                    hideGeoStory();
                } else {
                    hideAndShowStorySheet(geoStoryName);
                    showMarkerHighlight(geoStoryName, marker);
                    UserLogging.logViewGeoStory(geoStoryName);
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
                        marker.setIcon(GeoStoryMapPresenter.getIconByMatchValue(
                                geoStoryMatchMap.get(geoStoryName), true));
                    }
                }
            });
            markerHighlightInAnim.start();
            showGeoStoryTopInfoBar();
            // addHeroMarker(marker.getPosition());
        }
    }

    private void updateStorySheet(String geoStoryName) {
        float match = geoStoryMatchMap.get(geoStoryName);
        currentGeoStory = this.geoStoryMap.get(geoStoryName);
        nicknameView.setText(currentGeoStory.getUserNickname());
        neighborhoodView.setText(currentGeoStory.getNeighborhood());
        postedTimeView.setText(currentGeoStory.getRelativeDate());
        bioView.setText(currentGeoStory.getBio());
        imageAvatar.setImageResource(GeoStoryMapPresenter.getBitmapResource(match));

        setSimilarityText(match, similarityView);

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

        updateReactionViews(geoStoryName);
    }

    private void updateReactionViews(String geoStoryName) {
        boolean hasUserReacted = userReactionsMap.containsKey(geoStoryName);
        int numOfReactions = currentGeoStory.getNumReactions();

        if (hasUserReacted) {
            setReactionButtonState(true, userReactionsMap.get(geoStoryName));
        } else {
            setReactionButtonState(false, 0);
        }

        if (numOfReactions == 0) {
            numOfReactionsText.setText(R.string.geostory_zero_user_zero_others_reaction);
        } else {
            numOfReactionsText.setText(getNumberOfReactionsString(
                    numOfReactions, hasUserReacted, geoStoryName));
        }
    }

    private String getNumberOfReactionsString(int numReactions, boolean hasUserReacted,
                                              String geoStoryName) {
        if (hasUserReacted) {
            int userReactionId = userReactionsMap.get(geoStoryName);
            switch (numReactions) {
                case 1:
                    return getString(R.string.geostory_one_user_zero_others_reaction,
                            reactionEmotionNames[userReactionId]);
                case 2:
                    return getString(R.string.geostory_one_user_one_other_reaction);
                default:
                    return getString(R.string.geostory_one_user_many_others_reaction, numReactions);
            }
            /*
            if (numReactions == 1) {
                int userReactionId = userReactionsMap.get(geoStoryName);
                return getString(R.string.geostory_one_user_zero_others_reaction,
                        reactionEmotionNames[userReactionId]);
            } else {
                return getString(R.string.geostory_one_user_many_others_reaction, numReactions);
            }
            */
        } else {
            if (numReactions == 1) {
                return getString(R.string.geostory_zero_user_one_others_reaction);
            } else {
                return getString(R.string.geostory_zero_user_many_others_reaction, numReactions);
            }
        }
    }

    private void setSimilarityText(float match, TextView similarityView) {
        similarityView.setText(GeoStoryMapPresenter.getSimilarityText(match, getContext()));
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

    private void showExpandedStorySheet() {
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
                            } else {
                                currentGeoStory = null;
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
            UserLogging.logPlayGeoStory(currentGeoStory);
        }
    }

    private void stopPlayingResponse() {
        if (isPlayingStory && this.getActivity() != null) {
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
                setResolutionCompletedLocally();
                tryShowResolutionCompletedSnackbar();
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

    /* RESOLUTION METHODS */
    private void refreshResolutionInfo() {
        SharedPreferences prefs = WellnessIO.getSharedPref(getContext());
        boolean isShowResolutionGeoStory = prefs.getBoolean(KEY_SHOW_RESOLUTION_GEOSTORY, false);
        SynchronizedSetting setting = storywell.getSynchronizedSetting();

        if (isShowResolutionGeoStory) {
            setting.getResolutionInfo()
                    .setGeostoryResolutionStatus(GeoStoryResolutionStatus.WAITING_LISTENING);
            SynchronizedSettingRepository.saveLocalAndRemoteInstance(setting, getContext());
        }

        this.resolutionInfo = setting.getResolutionInfo();
    }

    private void setResolutionCompletedLocally() {
        if (GeoStoryResolutionStatus.NONE != resolutionInfo.getResolutionStatus()) {
            resolutionInfo.setResolutionStatus(GeoStoryResolutionStatus.WAITING_STORY_UNLOCK);
        }
    }

    private void tryShowResolutionInfoSnackbar() {
        /*
        if (GeoStoryResolutionStatus.WAITING_LISTENING == resolutionInfo.getResolutionStatus()) {
            showGeoStoryBottomInfoBar();
        }
        if (GeoStoryResolutionStatus.WAITING_STORY_UNLOCK == resolutionInfo.getResolutionStatus()) {
            showResolutionCompletedSnackbar();
        }
        */
        switch (resolutionInfo.getResolutionStatus()) {
            case GeoStoryResolutionStatus.WAITING_LISTENING:
                showGeoStoryBottomInfoBar();
                break;
            case GeoStoryResolutionStatus.WAITING_STORY_UNLOCK:
                showResolutionCompletedSnackbar();
                break;
        }
    }

    private void showGeoStoryTopInfoBar() {
        final int pixels = getPixelFromDp(-64);

        geoStoryTopInfoBar.setVisibility(View.INVISIBLE);
        geoStoryTopInfoBar.setTranslationY(pixels);
        geoStoryTopInfoBar.setVisibility(View.VISIBLE);

        ObjectAnimator animation = ObjectAnimator.ofFloat(
                geoStoryTopInfoBar, TRANSLATION_Y, 0);
        animation.setDuration(300);
        animation.setStartDelay(300);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator outAnimation = ObjectAnimator.ofFloat(
                        geoStoryTopInfoBar, TRANSLATION_Y, pixels);
                outAnimation.setStartDelay(2000);
                outAnimation.setDuration(300);
                outAnimation.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animation.start();
    }

    private void showGeoStoryBottomInfoBar() {
        float initialPosY = geoStoryBottomInfoBar.getTranslationY();
        int pixels = getPixelFromDp(48);

        geoStoryBottomInfoBar.setVisibility(View.INVISIBLE);
        geoStoryBottomInfoBar.setTranslationY(initialPosY - pixels);
        geoStoryBottomInfoBar.setVisibility(View.VISIBLE);

        ObjectAnimator animation = ObjectAnimator.ofFloat(
                geoStoryBottomInfoBar, "translationY", initialPosY);
        animation.setDuration(500);
        // animation.addListener();
        animation.start();
    }

    private void hideGeoStoryBottomInfoSnackbar() {
        geoStoryBottomInfoBar.setVisibility(View.GONE);
    }

    private void tryShowResolutionCompletedSnackbar() {
        if (GeoStoryResolutionStatus.WAITING_STORY_UNLOCK == resolutionInfo.getResolutionStatus()) {
            showResolutionCompletedSnackbar();
        }
    }

    private void showResolutionCompletedSnackbar() {
        int pixels = getPixelFromDp(-64);

        geoStoryActionBar.setVisibility(View.INVISIBLE);
        geoStoryActionBar.setTranslationY(pixels);
        geoStoryActionBar.setVisibility(View.VISIBLE);

        ObjectAnimator animation = ObjectAnimator.ofFloat(
                geoStoryActionBar, TRANSLATION_Y, ZERO_DP);
        animation.setDuration(500);
        animation.start();
    }

    private void hideResolutionCompletedSnackbar() {
        int pixels = getPixelFromDp(-64);

        ObjectAnimator animation = ObjectAnimator.ofFloat(geoStoryActionBar, TRANSLATION_Y, pixels);
        animation.setDuration(750);
        animation.start();
    }

    private void showUnlockStoryDialog(final View view) {
        final SynchronizedSetting setting = this.storywell.getSynchronizedSetting();
        if (setting.getStoryChallengeInfo().getIsSet()) {
            String title = setting.getStoryChallengeInfo().getStoryTitle();
            String coverImageUri = setting.getStoryChallengeInfo().getStoryCoverImageUri();
            AlertDialog dialog = ChallengeCompletedDialog.newInstance(
                    title, coverImageUri, view.getContext(),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            hideGeoStoryBottomInfoSnackbar();
                            hideResolutionCompletedSnackbar();
                            hideGeoStory();
                            doUnlockStory();
                            dialog.dismiss();
                        }
                    });
            dialog.show();
        }
    }

    private void doUnlockStory() {
        new CloseChallengeUnlockStoryAsync(getContext(), rootView,
                new CloseChallengeUnlockStoryAsync.OnUnlockingEvent() {

                    @Override
                    public void onClosingSuccess() {
                        HomeAdventurePresenter.setStoryChallengeAsClosed(getContext());
                        refreshResolutionInfo();
                        hideGeoStoryBottomInfoSnackbar();
                    }

                    @Override
                    public void onClosingFailed() {
                    }
                }).execute();
    }

    private int getPixelFromDp(int scalarDP) {
        return (int) (-64 * scaleDP + 0.5f);
    }

    /* NOTIFICATIONS METHODS */
    private void showFindUserSnackbar(View view, String geoStoryId, String userNickname) {
        String text = getString(R.string.geostory_find_user_text, userNickname);
        geoStoryMapSnackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
        geoStoryMapSnackbar.show();
    }

    /* REACTION METHODS */
    private void initReactionsMenu(View view) {
        Context wrapper = new ContextThemeWrapper(getContext(), R.style.AppTheme_Menu);
        reactionsMenu = new PopupMenu(wrapper, view);
        reactionEmotionNames = getResources().getStringArray(R.array.panas_positive_emotion_list);

        for (int i = 0; i < reactionEmotionNames.length; i++) {
            reactionsMenu.getMenu().add(1, i, i, reactionEmotionNames[i]);
        }

        reactionsMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onReactionMenuItemClick(item);
                return false;
            }
        });
    }

    private void showReactionsMenu() {
        if (userReactionsMap.containsKey(currentGeoStoryName)) {
            removeReactionToCurrentGeoStory(currentGeoStory);
        } else {
            reactionsMenu.show();
        }
    }

    private void onReactionMenuItemClick(MenuItem item) {
        setReactionToCurrentGeoStory(currentGeoStory, item.getItemId());
    }

    private void setReactionToCurrentGeoStory(GeoStory geoStory, int reactionId) {
        firebaseGeoStoryRepository = new FirebaseGeoStoryRepository(
                storywell.getGroup().getName(), geoStory.getMeta().getPromptParentId());
        firebaseGeoStoryRepository.addReaction(
                storywell.getGroup().getName(),
                storywell.getSynchronizedSetting().getFamilyInfo().getCaregiverNickname(),
                geoStory.getStoryId(),
                reactionId, geoStory.getUsername(),
                geoStory.getNumReactions() + 1);

        if (userReactionsMap.containsKey(geoStory.getStoryId())) {
            userReactionsMap.remove(geoStory.getStoryId());
            setReactionButtonState(false, reactionId);
        } else {
            userReactionsMap.put(geoStory.getStoryId(), reactionId);
            setReactionButtonState(true, reactionId);
            UserLogging.logAddGeoStoryReaction(
                    geoStory, reactionId, reactionEmotionNames[reactionId]);
        }

        // updateReactionViews(geoStoryName);
    }

    private void removeReactionToCurrentGeoStory(GeoStory geoStory) {
        firebaseGeoStoryRepository = new FirebaseGeoStoryRepository(
                storywell.getGroup().getName(), geoStory.getMeta().getPromptParentId());
        firebaseGeoStoryRepository.removeReaction(
                storywell.getGroup().getName(),
                geoStory.getStoryId());

        userReactionsMap.remove(geoStory.getStoryId());
        setReactionButtonState(false, 0);
    }

    private void setReactionButtonState(boolean isLiked, int reactionId) {
        if (isLiked) {
            buttonLike.setTextColor(getResources().getColor(R.color.colorPrimary));
            Drawable likeIcon = getResources().getDrawable(R.drawable.ic_thumb_up_active_24px);
            buttonLike.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    likeIcon, null, null, null);
            buttonLike.setText(reactionEmotionNames[reactionId]);
        } else {
            buttonLike.setTextColor(getResources().getColor(R.color.black));
            Drawable likeIcon = getResources().getDrawable(R.drawable.ic_thumb_up_24px);
            buttonLike.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    likeIcon, null, null, null);
            buttonLike.setText(R.string.button_geostory_reaction);
        }
    }

    private void showReactionsListDialog(View view) {
        ReactionsListDialog.newInstance(getContext(), currentGeoStory).show();
    }

    /* HELPERS */
    private String getNumberOfReactionsString(int numberOfReactions) {
        Resources res = getResources();
        return res.getQuantityString(R.plurals.number_of_reactions
                , numberOfReactions, numberOfReactions);
    }
}
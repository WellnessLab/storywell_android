package edu.neu.ccs.wellness.storytelling;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.geostory.UserGeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.viewmodel.StoryMapViewModel;

public class StoryMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    /* CONSTANTS */
    private static final String VIEW_LAT = "VIEW_LAT";
    private static final String VIEW_LONG = "VIEW_LONG";

    /* FIELDS */
    private CoordinatorLayout storyMapViewerSheet;
    private GoogleMap storyGoogleMap;
    private Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
    private Set<String> addedStorySet = new HashSet<>();
    private UserGeoStoryMeta userGeoStoryMeta;
    private String currentGeoStoryName = "";

    private BottomSheetBehavior geoStorySheetBehavior;

    private TextView postedTimeView;
    private TextView nicknameView;
    private TextView avgStepsView;
    private TextView neighborhoodView;
    private TextView bioView;

    /* CONSTRUCTOR */
    public StoryMapFragment() {
        // Required empty public constructor
    }

    /* FACTORY METHOD */
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StoryMapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StoryMapFragment newInstance(String param1, String param2) {
        StoryMapFragment fragment = new StoryMapFragment();
        Bundle args = new Bundle();
        args.putString(VIEW_LAT, param1);
        args.putString(VIEW_LONG, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /* INTERFACE METHODS */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StoryMapViewModel viewModel = ViewModelProviders.of(this)
                .get(StoryMapViewModel.class);

        LiveData<Map<String, GeoStory>> storyMapLiveData = viewModel.getStoryMapLiveData();
        storyMapLiveData.observe(this, new Observer<Map<String, GeoStory>>() {
            @Override
            public void onChanged(@Nullable Map<String, GeoStory> dataSnapshot) {
                updateStoryMap(dataSnapshot);
            }
        });

        final LiveData<UserGeoStoryMeta> userStoryMapMetaLiveData = viewModel.getUserStoryMetaLiveData(this.getContext());
        userStoryMapMetaLiveData.observe(this, new Observer<UserGeoStoryMeta>() {
            @Override
            public void onChanged(@Nullable UserGeoStoryMeta dataSnapshot) {
                userGeoStoryMeta = dataSnapshot;
            }
        });
    }

    private void updateStoryMap(Map<String, GeoStory> geoStoryMap) {
        this.geoStoryMap = geoStoryMap;
        populateMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_storymap, container, false);

        /* Prepare the bottom sheet to view stories */
        this.storyMapViewerSheet = rootView.findViewById(R.id.storymap_viewer_sheet);

        this.nicknameView = this.storyMapViewerSheet.findViewById(R.id.caregiver_nickname);
        this.avgStepsView = this.storyMapViewerSheet.findViewById(R.id.average_steps);
        this.postedTimeView = this.storyMapViewerSheet.findViewById(R.id.posted_time);
        this.neighborhoodView = this.storyMapViewerSheet.findViewById(R.id.neighborhood);
        this.bioView = this.storyMapViewerSheet.findViewById(R.id.user_bio);

        /* PREPARE THE STORY SHEET */
        this.geoStorySheetBehavior = BottomSheetBehavior.from(storyMapViewerSheet);
        this.geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        this.geoStorySheetBehavior.setHideable(true);

        // Set callback for changes
        this.geoStorySheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        if (!currentGeoStoryName.isEmpty()) {
                            updateStorySheet(currentGeoStoryName);
                            geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
        });

        /* Prepare the Bottom Sheet Behavior */
        View geoStoryOverview = this.storyMapViewerSheet.findViewById(R.id.overview);
        geoStoryOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(view.getId()) {
                    case R.id.overview:
                        toggleStorySheet();
                        break;
                    default:
                        break;
                }
            }
        });

        return rootView;
    }

    private void toggleStorySheet() {
                switch(geoStorySheetBehavior.getState()) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        geoStorySheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
        if(getActivity()!=null) {


            SupportMapFragment mapFragment = (SupportMapFragment)
                    getFragmentManager().findFragmentById(R.id.map);

            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        }
    }

    /**
     * Called when the {@link GoogleMap} is ready.
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        storyGoogleMap = googleMap;
        populateMap();
    }

    private void populateMap() {
        for (Map.Entry<String, GeoStory> entry : this.geoStoryMap.entrySet()) {
            String geoStoryName = entry.getKey();
            if (!this.addedStorySet.contains(geoStoryName)) {
                GeoStory geoStory = entry.getValue();
                float match = geoStory.getFitnessRatio(4000, 2000, 3000); // TODO
                boolean isViewed = !userGeoStoryMeta.isStoryUnread(geoStoryName);
                MarkerOptions markerOptions = StoryMapPresenter.getMarkerOptions(
                        geoStory, match, isViewed);
                Marker marker = storyGoogleMap.addMarker(markerOptions);
                marker.setTag(geoStoryName);
                this.addedStorySet.add(entry.getKey());
            }
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
        showGeoStory((String) marker.getTag());
        return false;
    }

    private void showGeoStory(String geoStoryName) {
        switch (this.geoStorySheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_HIDDEN:
                updateStorySheet(geoStoryName);
                showCollapsedStorySheet(geoStoryName);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
            case BottomSheetBehavior.STATE_EXPANDED:
                if (!currentGeoStoryName.equals(geoStoryName)) {
                    hideAndShowStorySheet(geoStoryName);
                }
                break;
            default:
                break;
        }
    }

    private void updateStorySheet(String geoStoryName) {
        GeoStory geoStory = this.geoStoryMap.get(geoStoryName);
        nicknameView.setText(geoStory.getUserNickname());
        avgStepsView.setText(geoStory.getSteps());
        postedTimeView.setText(geoStory.getRelativeDate());
        neighborhoodView.setText(geoStory.getNeighborhood());
        bioView.setText(geoStory.getBio());
    }

    private void hideAndShowStorySheet(String geoStoryName) {
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
}

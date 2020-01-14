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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.neu.ccs.wellness.storymap.GeoStory;
import edu.neu.ccs.wellness.storymap.UserGeoStoryMeta;
import edu.neu.ccs.wellness.storytelling.homeview.StoryMapPresenter;
import edu.neu.ccs.wellness.storytelling.viewmodel.StoryMapViewModel;

public class StoryMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private CoordinatorLayout storyMapViewerSheet;
    private MapView mapView;
    private GoogleMap storyGoogleMap;
    private Map<String, GeoStory> geoStoryMap = new ArrayMap<>();
    private Set<String> addedStorySet = new HashSet<>();
    private UserGeoStoryMeta userGeoStoryMeta;
    private String currentGeoStoryName = "";

    private BottomSheetBehavior geoStorySheetBehavior;

    private OnFragmentInteractionListener mListener;

    public StoryMapFragment() {
        // Required empty public constructor
    }

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
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

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

        CoordinatorLayout storyMapViewerSheet = rootView.findViewById(R.id.storymap_viewer_sheet);

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
                        if (currentGeoStoryName.isEmpty()) {
                            // Don't do anything
                        } else {
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
            public void onClick(View v) {
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
        });

        return rootView;
    }

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
    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        showGeoStory((String) marker.getTag());

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    private void showGeoStory(String geoStoryName) {
        switch (this.geoStorySheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_HIDDEN:
                showCollapsedStorySheet(geoStoryName);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
            case BottomSheetBehavior.STATE_EXPANDED:
                if (currentGeoStoryName.equals(geoStoryName)) {
                    // Do nothing
                } else {
                    hideAndShowStorySheet(geoStoryName);
                }
                break;
            default:
                break;
        }
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

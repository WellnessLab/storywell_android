package edu.neu.ccs.wellness.storytelling.storyview;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.utils.NearbyPlacesManagerInterface;
import edu.neu.ccs.wellness.storytelling.utils.PlaceItem;
import edu.neu.ccs.wellness.storytelling.utils.PlaceType;
import edu.neu.ccs.wellness.storytelling.utils.PlacesSearch;

public class EditLocationDialogFragment extends DialogFragment {

    /* CONSTANTS */
    public static final String TAG = "edit_geostory_location_dialog";

    /* INTERFACE */
    public interface GeoStoryLocationListener {
        void setLocationEdit(String placeName, Double lat, Double lng);
    }

    /* FIELDS */
    private GeoStoryLocationListener geostoryLocationListener;
    private NearbyPlacesManagerInterface nearbyPlacesManager;
    private ListView placesListview;
    private PlacesAdapter placesListAdapter;
    private PlacesSearch placesSearch;
    private PlaceItem realPlaceItem;

    private Toolbar toolbar;

    private LayoutInflater inflater;

    /* FACTORY METHODS */
    public static EditLocationDialogFragment newInstance(
            String realPlaceName, double realLat, double realLng) {
        EditLocationDialogFragment fragment = new EditLocationDialogFragment();
        fragment.realPlaceItem = new PlaceItem(realPlaceName, realLat, realLng);
        fragment.realPlaceItem.displayName = "Current location";
        fragment.realPlaceItem.isCurrentLocation = true;

        return fragment;
    }

    /* OVERRIDE METHODS */
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(
                R.layout.dialog_geostory_places, container, false);

        this.inflater = inflater;

        this.placesSearch = new PlacesSearch(getContext());
        this.placesListAdapter = new PlacesAdapter(getContext());

        this.toolbar = layout.findViewById(R.id.toolbar);
        this.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        this.toolbar.setTitle("Change Story Location");

        this.placesListview = layout.findViewById(R.id.places_list_view);
        this.placesListview.setAdapter(this.placesListAdapter);
        this.placesListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setGeoStoryLocation(position);
                dismiss();
            }
        });

        return layout;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);

        try {
            this.geostoryLocationListener = (GeoStoryLocationListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement GeoStoryLocationListener");
        }

        try {
            this.nearbyPlacesManager = (NearbyPlacesManagerInterface) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement NearbyPlacesManagerInterface");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Slide);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.nearbyPlacesManager.getPlaceItemList() != null) {
            this.placesListAdapter.refreshList(nearbyPlacesManager.getPlaceItemList());
        } else {
            new LoadPlacesAsync().execute();
        }
    }

    /* INTERNAL METHODS */
    private void setGeoStoryLocation(int position) {
        PlaceItem selectedPlace = placesListAdapter.getItem(position);
        this.geostoryLocationListener.setLocationEdit(selectedPlace.name, selectedPlace.lat, selectedPlace.lng);
        dismiss();
    }

    /* LIST ADAPTER */
    public class PlacesAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<PlaceItem> places = new ArrayList<>();

        public PlacesAdapter(Context context) {
            this.inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.places.add(realPlaceItem);
        }

        @Override
        public int getCount() {
            return this.places.size();
        }

        @Override
        public PlaceItem getItem(int position) {
            return this.places.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = inflater.inflate(R.layout.item_place, parent, false);
            TextView placeName = itemView.findViewById(R.id.place_name);
            ImageView placeIcon= itemView.findViewById(R.id.place_icon);


            PlaceItem placeItem = this.places.get(position);
            placeName.setText(placeItem.name);

            if (placeItem.isCurrentLocation) {
                placeIcon.setVisibility(View.VISIBLE);
            } else {
                placeIcon.setVisibility(View.INVISIBLE);
            }

            return itemView;
        }

        public void refreshList(List<PlaceItem> listOfPlaces) {
            this.places.clear();
            this.places.addAll(listOfPlaces);
            notifyDataSetChanged();
        }
    }

    /* PLACES SEARCH */
    private List<PlaceItem> getNearbyPlaces(double lat, double lng) {
        return this.placesSearch.getNearby(lat, lng, 5000, PlaceType.PARK);
    }

    /* ASYNCTASK */
    class LoadPlacesAsync extends AsyncTask<Void, Void, List<PlaceItem>> {

        @Override
        protected List<PlaceItem> doInBackground(Void... voids) {
            List<PlaceItem> placeItems = new ArrayList<>();
            placeItems.add(realPlaceItem);
            placeItems.addAll(getNearbyPlaces(realPlaceItem.lat, realPlaceItem.lng));
            return placeItems;
        }

        @Override
        protected void onPostExecute(List<PlaceItem> placeItems) {
            super.onPostExecute(placeItems);

            nearbyPlacesManager.setPlaceItemList(placeItems);
            placesListAdapter.refreshList(placeItems);
        }
    }
}

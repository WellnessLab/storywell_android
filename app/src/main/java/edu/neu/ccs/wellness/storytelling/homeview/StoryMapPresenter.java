package edu.neu.ccs.wellness.storytelling.homeview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.storytelling.R;

public class StoryMapPresenter {

    public static float HIGH_MATCH_CUTOFF = 0.6666f;
    public static float MODERATE_MATCH_CUTOFF = 0.3333f;
    public static final String TAG_HOME = "MARKER_HOME";
    private static final float MARKER_CENTER = 0.5f;
    private static final double MAX_OFFSET_DEGREE = 0.00072; // This is equal to 0.5 miles
    private static float INITIAL_ZOOM_PADDING = 0.015625f; // in degrees
    private static final int ONE = 1; // in pixel

    private static int MARKER_HIGH_DEFAULT = R.mipmap.geostory_marker_high_default;
    private static int MARKER_MOD_DEFAULT = R.mipmap.geostory_marker_moderate_default;
    private static int MARKER_LOW_DEFAULT = R.mipmap.geostory_marker_low_default;

    private static int MARKER_HIGH_ALERT = R.mipmap.geostory_marker_high_highlight;
    private static int MARKER_MOD_ALERT = R.mipmap.geostory_marker_moderate_highlight;
    private static int MARKER_LOW_ALERT = R.mipmap.geostory_marker_low_highlight;

    private static final int MIPMAP_MARKER_HOME = R.mipmap.geostory_marker_home;

    public static MarkerOptions getMarkerOptions(GeoStory geoStory, float match, boolean isViewed) {
        LatLng storyLatLang = new LatLng(geoStory.getLatitude(), geoStory.getLongitude());
        return new MarkerOptions()
                .position(storyLatLang)
                //.title(geoStory.getUserNickname())
                .icon(getIconByMatchValue(match, isViewed));
    }

    private static BitmapDescriptor getIconByMatchValue(float match, boolean isViewed) {
        if (isViewed) {
            return getViewedIcon(match);
        } else {
            return getUnviewedIcon(match);
        }
    }

    public static BitmapDescriptor getViewedIcon(float match) {
        if (match >= HIGH_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(MARKER_HIGH_DEFAULT);
        } else if (match >= MODERATE_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(MARKER_MOD_DEFAULT);
        } else {
            return BitmapDescriptorFactory.fromResource(MARKER_LOW_DEFAULT);
        }
    }

    private static BitmapDescriptor getUnviewedIcon(float match) {
        if (match >= HIGH_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(MARKER_HIGH_ALERT);
        } else if (match >= MODERATE_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(MARKER_MOD_ALERT);
        } else {
            return BitmapDescriptorFactory.fromResource(MARKER_LOW_ALERT);
        }
    }

    private static BitmapDescriptor getHomeIcon() {
        return BitmapDescriptorFactory.fromResource(MIPMAP_MARKER_HOME);
    }

    public static MarkerOptions getHomeMarker(LatLng homeLatLng) {
        return new MarkerOptions()
                .position(homeLatLng)
                .icon(StoryMapPresenter.getHomeIcon())
                .anchor(MARKER_CENTER, MARKER_CENTER);
    }

    public static MarkerOptions getSharingLocationMarker (LatLng homeLatLng) {
        return new MarkerOptions()
                .position(homeLatLng)
                .icon(BitmapDescriptorFactory.fromResource(MARKER_HIGH_DEFAULT));
    }

    public static boolean isAccessLocationGranted(Context context) {
        int permission = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get the a camera update centered on the current location.
     * @param context
     * @param defaultLocation
     * @return
     */
    public static CameraUpdate getCurrentLocCamera(Context context, LatLng defaultLocation) {
        return getCameraPosOnCenter(getCurrentLocation(context, defaultLocation));
    }

    /**
     * Get the a camera update centered on {@param center}
     * @param center
     * @return
     */
    private static CameraUpdate getCameraPosOnCenter(LatLng center) {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(new LatLng(
                        center.latitude - INITIAL_ZOOM_PADDING,
                        center.longitude - INITIAL_ZOOM_PADDING))
                .include(new LatLng(
                        center.latitude + INITIAL_ZOOM_PADDING,
                        center.longitude + INITIAL_ZOOM_PADDING))
                .build();
        return CameraUpdateFactory.newLatLngBounds(bounds, ONE);
    }

    /**
     * Retrieve the user's location using their last known location.
     * @param context
     * @param defaultLocation
     * @return
     */
    @SuppressLint("MissingPermission")
    public static LatLng getCurrentLocation(Context context, LatLng defaultLocation) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager
                    .getBestProvider(criteria, false));
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            return defaultLocation;
        }
    }

    /**
     * Creates a new {@link Location} object that offsets the latitude and longitude by at most
     * {@value MAX_OFFSET_DEGREE}.
     * @param location
     * @return An offset Location.
     */
    public static Location getOffsetLocation(Location location) {
        double latOffset = Math.random() + MAX_OFFSET_DEGREE;
        double lngOffset = Math.random() + MAX_OFFSET_DEGREE;

        Location offsetLocation = new Location("anyprovider");
        offsetLocation.setLatitude(location.getLatitude() + latOffset);
        offsetLocation.setLongitude(location.getLongitude() + lngOffset);

        return offsetLocation;
    }
}

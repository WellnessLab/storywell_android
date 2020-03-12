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

public class StoryMapPresenter {

    public static float HIGH_MATCH_CUTOFF = 0.6666f;
    public static float MODERATE_MATCH_CUTOFF = 0.3333f;
    public static final String TAG_HOME = "MARKER_HOME";
    private static final float MARKER_CENTER = 0.5f;
    private static final double MAX_LAT_OFFSET_DEGREE = 0.0029; // This is equal to 0.2 miles
    private static final double MAX_LNG_OFFSET_DEGREE = 0.0033; // This is equal to 0.2 miles
    private static float INITIAL_ZOOM_PADDING = 0.015625f; // in degrees
    private static final int ONE = 1; // in pixel

    public static MarkerOptions getHomeMarker(LatLng homeLatLng) {
        return new MarkerOptions()
                .position(homeLatLng)
                .icon(BitmapDescriptorFactory.fromResource(GeoStoryIcons.HOME))
                .anchor(MARKER_CENTER, MARKER_CENTER);
    }

    public static MarkerOptions getMarkerOptions(GeoStory geoStory, float match, boolean isViewed) {
        LatLng storyLatLang = new LatLng(geoStory.getLatitude(), geoStory.getLongitude());
        return new MarkerOptions()
                .position(storyLatLang)
                .icon(getIconByMatchValue(match, isViewed));
    }

    public static MarkerOptions getMarkerOptionsById(GeoStory geoStory, int iconId) {
        LatLng storyLatLang = new LatLng(geoStory.getLatitude(), geoStory.getLongitude());
        return new MarkerOptions()
                .position(storyLatLang)
                .icon(getStoryIcon(iconId));
    }

    public static BitmapDescriptor getStoryIcon(int iconId) {
        return BitmapDescriptorFactory.fromResource(getIconRes(iconId));
    }

    public static int getIconRes(int iconId) {
        return GeoStoryIcons.ICONS[iconId];
    }

    public static BitmapDescriptor getIconByMatchValue(float match, boolean isViewed) {
        if (isViewed) {
            return BitmapDescriptorFactory.fromResource(getBitmapResource(match));
        } else {
            return getUnviewedIcon(match);
        }
    }

    public static int getBitmapResource(float match) {
        if (match >= HIGH_MATCH_CUTOFF) {
            return GeoStoryIcons.MARKERS[2];
        } else if (match >= MODERATE_MATCH_CUTOFF) {
            return GeoStoryIcons.MARKERS[1];
        } else {
            return GeoStoryIcons.MARKERS[0];
        }
    }

    private static BitmapDescriptor getUnviewedIcon(float match) {
        if (match >= HIGH_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(GeoStoryIcons.MARKERS_UNREAD[2]);
        } else if (match >= MODERATE_MATCH_CUTOFF) {
            return BitmapDescriptorFactory.fromResource(GeoStoryIcons.MARKERS_UNREAD[1]);
        } else {
            return BitmapDescriptorFactory.fromResource(GeoStoryIcons.MARKERS_UNREAD[0]);
        }
    }

    public static MarkerOptions getSharingLocationMarker (LatLng latLng) {
        return new MarkerOptions()
                .position(latLng)
                .title("We offset the location to protect your privacy")
                .icon(getStoryIcon(0));
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
                        center.latitude + INITIAL_ZOOM_PADDING,
                        center.longitude - INITIAL_ZOOM_PADDING))
                .include(new LatLng(
                        center.latitude - INITIAL_ZOOM_PADDING,
                        center.longitude + INITIAL_ZOOM_PADDING))
                .build();
        //return CameraUpdateFactory.newLatLngBounds(bounds, ONE);
        return CameraUpdateFactory.newLatLngZoom(center, 15);
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
     * {@value MAX_LAT_OFFSET_DEGREE} and {@value MAX_LNG_OFFSET_DEGREE}.
     * @param location
     * @return An offset Location.
     */
    public static Location getOffsetLocation(Location location) {
        double latOffset = (2 * (Math.random() - 0.5)) * MAX_LAT_OFFSET_DEGREE;
        double lngOffset = (2 * (Math.random() - 0.5)) * MAX_LNG_OFFSET_DEGREE;

        return getLocationFromLatLng(location.getLatitude() + latOffset,
                location.getLongitude() + lngOffset);
    }

    public static Location getLocationFromLatLng(double lat, double lng) {
        Location location = new Location("anyprovider");
        location.setLatitude(lat);
        location.setLongitude(lng);

        return location;
    }
}

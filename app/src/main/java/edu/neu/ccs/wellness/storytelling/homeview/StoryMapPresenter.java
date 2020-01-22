package edu.neu.ccs.wellness.storytelling.homeview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.storytelling.R;

public class StoryMapPresenter {

    public static float HIGH_MATCH_CUTOFF = 0.6666f;
    public static float MODERATE_MATCH_CUTOFF = 0.3333f;

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

    private static BitmapDescriptor getViewedIcon(float match) {
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

    public static BitmapDescriptor getHomeIcon(Context context) {
        return BitmapDescriptorFactory.fromResource(MIPMAP_MARKER_HOME);
        //return bitmapDescriptorFromVector(context, MARKER_HOME);
    }
    /*
    private static BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    */
}

package edu.neu.ccs.wellness.storytelling.homeview;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.neu.ccs.wellness.geostory.GeoStory;
import edu.neu.ccs.wellness.storytelling.R;

public class StoryMapPresenter {

    public static float HIGH_MATCH_CUTOFF = 0.6666f;
    public static float MODERATE_MATCH_CUTOFF = 0.3333f;

    private static int MARKER_HIGH_DEFAULT = R.drawable.ic_story_marker_large_high_match_default;
    private static int MARKER_MOD_DEFAULT = R.drawable.ic_story_marker_large_mod_match_default;
    private static int MARKER_LOW_DEFAULT = R.drawable.ic_story_marker_large_low_match_default;

    private static int MARKER_HIGH_ALERT = R.drawable.ic_story_marker_large_high_match_highlight;
    private static int MARKER_MOD_ALERT = R.drawable.ic_story_marker_large_mod_match_highlight;
    private static int MARKER_LOW_ALERT = R.drawable.ic_story_marker_large_low_match_highlight;

    private static int MARKER_HOME = R.drawable.ic_home_marker_large;

    public static MarkerOptions getMarkerOptions(GeoStory geoStory, float match, boolean isViewed) {
        LatLng storyLatLang = new LatLng(geoStory.getLatitude(), geoStory.getLongitude());
        return new MarkerOptions()
                .position(storyLatLang)
                .title(geoStory.getUserNickname())
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

    public static BitmapDescriptor getHomeIcon() {
        return BitmapDescriptorFactory.fromResource(MARKER_HOME);
    }
}

package edu.neu.ccs.wellness.storymap;

import android.text.format.DateUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.Date;
import java.util.Map;

@IgnoreExtraProperties
public class GeoStory {
    public static final String KEY_LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String KEY_IS_REVIEWED = "isReviewed";

    private String storyId = "";
    private double latitude = 0;
    private double longitude = 0;
    private String username = "default";
    private long lastUpdateTimestamp = 0;
    private String storyUri = "";
    private boolean isReviewed = false;
    private Map<String, Object> reviewMeta;
    private GeoStoryMeta meta;

    /* CONSTRUCTORS */
    public GeoStory() {

    }

    public GeoStory(String storyId, double latitude, double longitude, String username,
                    String storyUri, boolean isReviewed, GeoStoryMeta meta) {
        this.storyId = storyId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.username = username;
        this.storyUri = storyUri;
        this.isReviewed = isReviewed;
        this.meta = meta;
    }

    /* GETTER AND SETTER METHODS */
    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public String getStoryUri() {
        return storyUri;
    }

    public void setStoryUri(String storyUri) {
        this.storyUri = storyUri;
    }

    @PropertyName(KEY_IS_REVIEWED)
    public boolean isReviewed() {
        return isReviewed;
    }

    public void setReviewed(boolean reviewed) {
        isReviewed = reviewed;
    }

    public Map<String, Object> getReviewMeta() {
        return reviewMeta;
    }

    public void setReviewMeta(Map<String, Object> reviewMeta) {
        this.reviewMeta = reviewMeta;
    }

    public GeoStoryMeta getMeta() {
        return meta;
    }

    public void setMeta(GeoStoryMeta meta) {
        this.meta = meta;
    }

    /* METHODS */
    public Date getLastUpdateDate() {
        return new Date(this.getLastUpdateTimestamp());
    }

    @Exclude
    public String getRelativeDate() {
        return (String) DateUtils.getRelativeTimeSpanString(
                this.getLastUpdateTimestamp(),
                System.currentTimeMillis(),
                5
        );
    }

    @Exclude
    public int getSteps() {
        if (this.meta != null) {
            return this.meta.getAverageSteps();
        } else {
            return GeoStoryMeta.DEFAULT_STEPS;
        }
    }

    @Exclude
    public String getBio() {
        if (this.meta != null) {
            return this.meta.getBio();
        } else {
            return GeoStoryMeta.DEFAULT_BIO;
        }
    }

    @Exclude
    public float getFitnessRatio(int steps, int min, int max) {
        if (this.meta != null) {
            return this.meta.getFitnessRatio(steps, min, max);
        } else {
            return GeoStoryMeta.MIN_RATIO;
        }
    }

    @Exclude
    public String getUserNickname() {
        if (this.meta != null) {
            return this.meta.getUserNickname();
        } else {
            return GeoStoryMeta.DEFAULT_NICKNAME;
        }
    }

    @Exclude
    public String getNeighborhood() {
        if (this.meta != null) {
            return this.meta.getNeighborhood();
        } else {
            return GeoStoryMeta.DEFAULT_NEIGHBORHOOD;
        }
    }
}

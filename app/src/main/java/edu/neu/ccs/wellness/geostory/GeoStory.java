package edu.neu.ccs.wellness.geostory;

import android.text.format.DateUtils;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@IgnoreExtraProperties
public class GeoStory {
    public static final String KEY_LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String KEY_IS_REVIEWED = "isReviewed";
    public static final String FILENAME = "geostory_userId_%s__promptParentId_%s__promptId_%s__%s.mp4";
    public static final String KEY = "%s_%s";
    private static final String DATE_FORMAT ="yyyy-MM-dd_HH-mm-ss";

    private String storyId = "";
    private double latitude = 0;
    private double longitude = 0;
    private String username = "default";
    private long lastUpdateTimestamp = 0;
    private String storyUri = "";
    private String gsUri = "";
    private boolean isReviewed = false;
    private Map<String, Object> reviewMeta;
    private GeoStoryMeta meta = new GeoStoryMeta();

    @Exclude private String dateString = "1970-01-01_00-00-00";

    /* CONSTRUCTORS */
    public GeoStory() {
        this.setLastUpdateTimestamp(Calendar.getInstance(Locale.US).getTimeInMillis());
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
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTimeInMillis(this.lastUpdateTimestamp);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        this.dateString = dateFormatter.format(cal.getTime());
    }

    public String getStoryUri() {
        return storyUri;
    }

    public void setStoryUri(String storyUri) {
        this.storyUri = storyUri;
    }

    public String getGsUri() {
        return gsUri;
    }

    public void setGsUri(String gsUri) {
        this.gsUri = gsUri;
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

    @Exclude
    public String getFilename() {
        return String.format(FILENAME, this.getUsername(),
                this.getMeta().getPromptParentId(), this.getMeta().getPromptId(), this.dateString);
    }
}

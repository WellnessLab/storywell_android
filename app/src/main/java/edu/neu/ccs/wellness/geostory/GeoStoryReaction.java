package edu.neu.ccs.wellness.geostory;

import java.util.Calendar;
import java.util.Locale;

public class GeoStoryReaction {
    private String userId;
    private String userNickname;
    private String geoStoryId;
    private String geoStoryAuthor;
    private long timestamp;
    private int reactionId;
    private int totalReactions = 0;

    /* CONSTRUCTORS */
    public GeoStoryReaction() {

    }

    public GeoStoryReaction(String userId, String userNickname, String geoStoryId, int reactionId,
                            int totalReactions, String geoStoryAuthor) {
        this.userId = userId;
        this.userNickname = userNickname;
        this.geoStoryId = geoStoryId;
        this.reactionId = reactionId;
        this.timestamp = Calendar.getInstance(Locale.US).getTimeInMillis();
        this.totalReactions = totalReactions;
        this.geoStoryAuthor = geoStoryAuthor;
    }


    public String getUserId() {
        return userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public String getGeoStoryId() {
        return geoStoryId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getReactionId() {
        return reactionId;
    }

    public int getTotalReactions() {
        return totalReactions;
    }

    public String getGeoStoryAuthor() {
        return geoStoryAuthor;
    }
}

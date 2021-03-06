package edu.neu.ccs.wellness.reflection;

import java.util.Map;

/**
 * Created by hermansaksono on 12/7/18.
 */

public class ResponsePile {
    public static final String KEY_RESPONSE_GROUP_NAME = "responseGroupName";
    public static final String KEY_RESPONSE_TIMESTAMP = "responseTimestamp";
    public static final String KEY_RESPONSE_TYPE = "type";
    public static final String KEY_RESPONSE_PILE = "piles";
    public static final String DEFAULT_RESPONSE_GROUP_NAME = "";

    private static final String TO_STRING_FORMAT = "story_%s_reflection:_%s";

    // private int incarnationId;
    private int type = TreasureItemType.STORY_REFLECTION;
    private int storyId;
    private String title;
    private Map<String, String> piles;
    private long timestampUpdatedOn;

    /* CONSTRUCTORS */
    public ResponsePile() {

    }

    public ResponsePile(int storyId, String storyTitle, Map<String, String> piles, long timestamp, int type) {
        this.storyId = storyId;
        this.title = storyTitle;
        this.piles = piles;
        this.timestampUpdatedOn = timestamp;
        this.type = type;
    }

    /* GETTER AND SETTER */
    public int getStoryId() {
        return storyId;
    }

    public void setStoryId(int storyId) {
        this.storyId = storyId;
    }

    public Map<String, String> getPiles() {
        return this.piles;
    }

    public void setPiles(Map<String, String> piles) {
        this.piles = piles;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return this.timestampUpdatedOn;
    }

    public int getType() {
        return this.type;
    }

    /* DEFAULT METHODS */
    @Override
    public String toString () {
        return String.format(TO_STRING_FORMAT, this.storyId, this.piles.toString());
    }
}

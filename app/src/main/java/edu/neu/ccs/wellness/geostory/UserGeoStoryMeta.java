package edu.neu.ccs.wellness.geostory;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class UserGeoStoryMeta {
    private static final String[] DEFAULT_UNREAD_STORIES = {"0"};
    public static final String KEY_READ_STORIES = "readStories";

    private String username = "default";
    private Map<String, String> readStories = new HashMap<>();

    /* CONSTRUCTOR */
    public UserGeoStoryMeta() {
    }

    /* GETTER AND SETTER METHODS */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getUnreadStories() {
        return readStories;
    }

    public void setUnreadStories(Map<String, String> unreadStories) {
        this.readStories = unreadStories;
    }

    /* PUBLIC METHOD */
    @Exclude
    public boolean isStoryRead(String storyId) {
        return this.readStories.containsKey(storyId);
    }
}

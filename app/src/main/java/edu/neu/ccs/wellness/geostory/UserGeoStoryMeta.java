package edu.neu.ccs.wellness.geostory;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@IgnoreExtraProperties
public class UserGeoStoryMeta {
    private static final String[] DEFAULT_UNREAD_STORIES = {"0"};
    public static final String KEY_READ_STORIES = "readStories";

    private String username = "default";
    private List<String> readStories = new ArrayList<>(Arrays.asList(DEFAULT_UNREAD_STORIES));

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

    public List<String> getUnreadStories() {
        return readStories;
    }

    public void setUnreadStories(List<String> unreadStories) {
        this.readStories = unreadStories;
    }

    /* PUBLIC METHOD */
    @Exclude
    public boolean isStoryRead(String storyId) {
        return this.readStories.contains(storyId);
    }
}

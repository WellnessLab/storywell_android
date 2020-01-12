package edu.neu.ccs.wellness.storymap;

public class GeoStory {

    private double latitude = 0;
    private double longitude = 0;
    private String username = "default";
    private String storyUrl = "";
    private boolean isReviewed = false;

    public GeoStory() {

    }

    public GeoStory(double latitude, double longitude, String username, String storyUrl, boolean isReviewed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.username = username;
        this.storyUrl = storyUrl;
        this.isReviewed = isReviewed;
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

    public String getStoryUrl() {
        return storyUrl;
    }

    public void setStoryUrl(String storyUrl) {
        this.storyUrl = storyUrl;
    }

    public boolean isReviewed() {
        return isReviewed;
    }

    public void setReviewed(boolean reviewed) {
        isReviewed = reviewed;
    }
}

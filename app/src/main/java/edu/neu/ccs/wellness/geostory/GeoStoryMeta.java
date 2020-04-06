package edu.neu.ccs.wellness.geostory;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class GeoStoryMeta {

    private static final String KEY_IS_SHOW_AVG_STEPS = "isShowAverageSteps";
    private static final String KEY_IS_SHOW_NEIGHBORHOOD = "isShowNeighborhood";

    public static int DEFAULT_STEPS  = 0;
    public static String DEFAULT_BIO = "";
    public static String DEFAULT_NEIGHBORHOOD = "Boston";
    public static String DEFAULT_NICKNAME = "Neighbor";
    public static float MIN_RATIO = 0f;
    public static float OPTIMUM_RATIO = 1f;

    private int averageSteps = DEFAULT_STEPS;
    private String bio = DEFAULT_BIO;
    private String neighborhood = DEFAULT_NEIGHBORHOOD;
    private String userNickname = DEFAULT_NICKNAME;
    private String promptParentId = "0";
    private String promptId = "0";
    private String transcript = "";
    private int iconId = 0;

    private int numComments = 0;
    private int numReactions = 0;

    private boolean isShowAverageSteps = true;
    private boolean isShowNeighborhood = true;

    private double originalLatitude = 0;
    private double originalLongitude = 0;

    /* CONSTRUCTOR */
    public GeoStoryMeta() {

    }

    public GeoStoryMeta(int averageSteps, String bio) {
        this.averageSteps = averageSteps;

        if (bio != null) {
            this.bio = bio;
        }
    }

    /* GETTER AND SETTER METHODS */
    public int getAverageSteps() {
        return averageSteps;
    }

    public void setAverageSteps(int averageSteps) {
        this.averageSteps = averageSteps;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getPromptParentId() {
        return promptParentId;
    }

    public void setPromptParentId(String promptParentId) {
        this.promptParentId = promptParentId;
    }

    public String getPromptId() {
        return promptId;
    }

    public void setPromptId(String promptId) {
        this.promptId = promptId;
    }

    public String getTranscript() { return transcript; }

    public void setTranscript(String transcript) { this.transcript = transcript; }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public double getOriginalLatitude() {
        return originalLatitude;
    }

    public void setOriginalLatitude(double originalLatitude) {
        this.originalLatitude = originalLatitude;
    }

    public double getOriginalLongitude() {
        return originalLongitude;
    }

    public void setOriginalLongitude(double originalLongitude) {
        this.originalLongitude = originalLongitude;
    }

    public int getNumReactions() {
        return numReactions;
    }

    public void setNumReactions(int numReactions) {
        this.numReactions = numReactions;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    /* BOOLEAN TOGGLING METHODS */
    @PropertyName(KEY_IS_SHOW_AVG_STEPS)
    public boolean isShowAverageSteps() {
        return isShowAverageSteps;
    }

    public void setShowAverageSteps(boolean showAverageSteps) {
        isShowAverageSteps = showAverageSteps;
    }

    @PropertyName(KEY_IS_SHOW_NEIGHBORHOOD)
    public boolean isShowNeighborhood() {
        return isShowNeighborhood;
    }

    public void setShowNeighborhood(boolean showNeighborhood) {
        isShowNeighborhood = showNeighborhood;
    }

    /* METHODS */
    /**
     * Returns the ratio of fitness similarity between the given user {@param steps} with the
     * {@link GeoStory} author's steps. 1.0 means very similar and 0.0 means not similar at all.
     * @param steps Another users' steps count average.
     * @param min The population's minimum steps average.
     * @param max The population's maximum steps average.
     * @return The similarity ratio.
     */
    @Exclude
    public float getFitnessRatio(int steps, int min, int max) {
        if (min == max) {
            return 1.0f;
        } else {
            float range = max - min;
            int thisUser = this.averageSteps - min;
            int givenUser = steps - min;

            return Math.abs(OPTIMUM_RATIO - ((thisUser - givenUser) / range)); // TODO Need work
        }
    }
}

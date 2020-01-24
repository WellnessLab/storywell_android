package edu.neu.ccs.wellness.geostory;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class GeoStoryMeta {

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
    private int iconId = 0;

    private boolean isShowAverageSteps = true;
    private boolean isShowNeighborhood = true;

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

    /* BOOLEAN TOGGLING METHODS */
    public boolean isShowAverageSteps() {
        return isShowAverageSteps;
    }

    public void setShowAverageSteps(boolean showAverageSteps) {
        isShowAverageSteps = showAverageSteps;
    }

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
        float range = max - min;
        int thisUser = this.averageSteps - min;
        int givenUser = steps - min;

        return Math.abs(OPTIMUM_RATIO - ((thisUser - givenUser) / range)); // TODO Need work
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }
}

package edu.neu.ccs.wellness.storymap;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class GeoStoryMeta {

    public static int DEFAULT_STEPS  = 0;
    public static String DEFAULT_BIO = "";
    public static float MIN_RATIO = 0f;
    public static float OPTIMUM_RATIO = 1f;

    private int averageSteps = DEFAULT_STEPS;
    private String bio = DEFAULT_BIO;

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
}

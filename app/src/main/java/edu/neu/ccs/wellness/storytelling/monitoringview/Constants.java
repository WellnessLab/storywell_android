package edu.neu.ccs.wellness.storytelling.monitoringview;

import edu.neu.ccs.wellness.storytelling.R;

/**
 * Created by hermansaksono on 2/6/19.
 */

public class Constants {

    public final static float DEFAULT_FPS = 30;
    public final static int MICROSECONDS = 1000;

    public final static int NUM_HERO_DRAWABLES = 2;
    public final static int HERO_DRAWABLE_FLYING = 0;
    public final static int HERO_DRAWABLE_COMPLETE = 1;
    public static final int NUM_BALLOONS_PER_PERSON = 10;
    public static final float MINIMUM_PROGRESS_FOR_ANIMATION = 0.025f;

    public static final int DEFAULT_FEMALE_HERO = 0;
    public static final int DEFAULT_MALE_HERO = 1;

    private static final int[] MIRA_DRAWABLES = {
            R.drawable.art_hero_mira,
            R.drawable.art_hero_mira_completed};

    private static final int[] DIEGO_DRAWABLES = {
            R.drawable.art_hero_diego,
            R.drawable.art_hero_diego_completed};

    public static final int[][] HERO_DRAWABLES = {
            MIRA_DRAWABLES,
            DIEGO_DRAWABLES};

    public static final int SUNRAY_DRAWABLE = R.drawable.art_gameview_sunflare_light;
    public static final float SUNRAY_MOVE_UP_DIST = 15;
    public static final float SUNRAY_MOVE_UP_SECONDS = 0.6f;
    public static final float SUNRAY_MAX_UP = 30;

    public static final float SUNRAY_FADEIN_SECONDS = 0.5f;
    public static final float SUNRAY_ROTATE_TIME = 1f;
    public static final int SUNRAY_ROTATE_STEP = (int) (MICROSECONDS
            * (SUNRAY_ROTATE_TIME / DEFAULT_FPS));

    /* Animation related values */
    public static final float ANIM_BALLOON_UPDATE_PERIOD = 2; // seconds per hover
    static final float ANIM_HOVER_RANGE = 5;  // dp per seconds
    static final float ANIM_BOUNCE_RANGE = 20;  // dp per seconds
    static final float ANIM_HOVER_PERIOD = 4; // seconds per hover
    public static final float ANIM_MOVING_PERIOD = 5;  // seconds to reach destination
    static final float ANIM_ARC_GAP_PERIOD = 4;   // seconds to reach destination
    static final float ANIM_BOUNCE_PERIOD = 0.6f;  // seconds to reach destination
    static final float ANIM_HALF_BOUNCE_PERIOD = 0.5f;  // seconds to reach destination
}

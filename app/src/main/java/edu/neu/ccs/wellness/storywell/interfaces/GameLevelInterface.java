package edu.neu.ccs.wellness.storywell.interfaces;

import android.content.res.Resources;
import android.graphics.Paint;
import android.util.Pair;

import edu.neu.ccs.wellness.storywell.monitoringview.HeroSprite;

/**
 * Created by hermansaksono on 2/13/18.
 */

public interface GameLevelInterface {
    GameBackgroundInterface getBaseBackground(Resources res);

    GameSpriteInterface getIsland(Resources res, int dayOfWeek,
                                  float posXRatio, float posYRatio, float scaleRatio);

    GameSpriteInterface getSeaFg(Resources res, float rangeXRatio, float rangeYRatio);

    GameSpriteInterface getCloudBg1(Resources res);

    GameSpriteInterface getCloudBg2(Resources res);

    GameSpriteInterface getCloudFg1(Resources res);

    GameSpriteInterface getCloudFg2(Resources res);
}

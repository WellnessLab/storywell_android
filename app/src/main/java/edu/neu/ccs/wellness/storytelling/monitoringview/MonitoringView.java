package edu.neu.ccs.wellness.storytelling.monitoringview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import edu.neu.ccs.wellness.storytelling.R;
import edu.neu.ccs.wellness.storytelling.monitoringview.interfaces.GameBackgroundInterface;
import edu.neu.ccs.wellness.storytelling.monitoringview.interfaces.GameSpriteInterface;
import edu.neu.ccs.wellness.storytelling.monitoringview.interfaces.GameViewInterface;
import edu.neu.ccs.wellness.utils.WellnessGraphics;

/**
 * Created by hermansaksono on 2/8/18.
 */

public class MonitoringView extends View implements GameViewInterface {
    /* STATIC VARIABLES */
    public final static int MICROSECONDS = 1000;
    private final static float DEFAULT_FPS = Constants.DEFAULT_FPS;

    /* PRIVATE VARIABLES */
    private int width;
    private int height;
    private float density;
    private float fps = DEFAULT_FPS;
    private int delay = (int) (1000 / fps);
    private long pauseBeginMillisec = 0;
    private long startMillisec = 0;
    private boolean isRunning = false;
    private Handler handler = new Handler();
    private GameAnimationThread animationThread;
    private List<GameBackgroundInterface> backgrounds = new ArrayList<GameBackgroundInterface>();
    private List<GameSpriteInterface> sprites = new ArrayList<GameSpriteInterface>();
    private int numDays;

    /* CONSTRUCTOR */
    public MonitoringView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.density = WellnessGraphics.getPixelDensity(context);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.MonitoringView, 0, 0);
        try {
            this.numDays = typedArray.getInteger(R.styleable.MonitoringView_num_days,0);
        } finally {
            typedArray.recycle();
        }
    }

    /* VIEW METHODS */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.width = MeasureSpec.getSize(widthMeasureSpec);
        this.height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(this.width, this.height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        this.width = width;
        this.height = height;
        float density = WellnessGraphics.getPixelDensity(getContext());
        updateSizeChange(width, height, backgrounds, sprites, density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.scale((float)1.5, (float)1.5, this.width/2, this.height/2);
        drawBackgrounds(canvas, backgrounds);
        drawSprites(canvas, sprites);
    }

    /* PUBLIC INTERFACE METHODS */
    @Override
    public int getNumDays() { return this.numDays; }

    /**
     * Add a background to the @SevenDayMonitoringView.
     * @param background A @GameBackgroundInterface object that is going to be added.
     */
    @Override
    public void addBackground(GameBackgroundInterface background) {
        background.onAttach(this.width, this.height);
        this.backgrounds.add(background);
    }

    /**
     * Remove a background from the @SevenDayMonitoringView.
     * @param background A @GameBackgroundInterface object that is going to be added.
     */
    @Override
    public void removeBackground(GameBackgroundInterface background) {
        this.backgrounds.remove(background);
    }

    /**
     * Get a list of background in @SevenDayMonitoringView.
     * @return The backgrounds.
     */
    @Override
    public List<GameBackgroundInterface> getListOfBackground() {
        return this.backgrounds;
    }

    /**
     * Add a sprite to the @SevenDayMonitoringView
     * @param sprite A @GameSpriteInterface object that is going to be added.
     */
    @Override
    public void addSprite(GameSpriteInterface sprite) {
        this.sprites.add(sprite);
    }

    /**
     * Remove a sprite from the @SevenDayMonitoringView
     * @param sprite A @GameBackgroundInterface object that is going to be added.
     */
    @Override
    public void removeSprite(GameSpriteInterface sprite) {
        this.sprites.remove(sprite);
    }

    /**
     * Get a list of sprites in @SevenDayMonitoringView.
     * @return The sprites.
     */
    @Override
    public List<GameSpriteInterface> getListOfSprite() {
        return this.sprites;
    }

    @Override
    public void start() {
        this.animationThread = new GameAnimationThread();
    }

    @Override
    public void resume() {
        if (this.pauseBeginMillisec == 0) {
            this.startMillisec = SystemClock.uptimeMillis();
        } else {
            this.startMillisec += SystemClock.uptimeMillis() - pauseBeginMillisec;
        }
        this.isRunning = true;
        this.handler.post(this.animationThread);
    }

    @Override
    public void pause() {
        this.isRunning = false;
        this.pauseBeginMillisec = SystemClock.uptimeMillis();
    }

    @Override
    public void stop() {
        this.handler.removeCallbacks(this.animationThread);
    }

    @Override
    public boolean isPlaying() {
        return this.isRunning;
    }

    public boolean isOverAnyIsland(MotionEvent event) {
        for (GameSpriteInterface oneSprite : this.sprites) {
            if (oneSprite.getClass() == IslandSprite.class){
                if (oneSprite.isOver(event.getX(), event.getY()) == true) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOverHero(MotionEvent event) {
        for (GameSpriteInterface oneSprite : this.sprites) {
            if (oneSprite.getClass() == HeroSprite.class){
                if (oneSprite.isOver(event.getX(), event.getY())) {
                    HeroSprite heroSprite = (HeroSprite) oneSprite;
                    return heroSprite.getIsVisible();
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Updates the contents of this @GameViewInterface
     */
    @Override
    public void update(long millisec) {
        updateBackgrounds(this.backgrounds, millisec);
        updateSprites(this.sprites, millisec, this.density);
    }

    @Override
    public long getElapsedMillisec() {
        if (this.animationThread != null) {
            return this.animationThread.getElapsedMillisec();
        } else {
            return 0;
        }
    }

    public int getDayIndex(float touchPosX) {
        int totalIslands = this.numDays + 1;
        float islandWidth = this.width / (float) totalIslands;
        float tapXRatio = touchPosX / islandWidth;
        return (int) Math.floor(tapXRatio) + 1;
    }

    /* ANIMATION THREAD */
    private class GameAnimationThread implements Runnable {
        @Override
        public void run() {
            long elapsed = (SystemClock.uptimeMillis() - startMillisec);
            if (isRunning) {
                update(elapsed);
                invalidate();
                handler.postDelayed(this, delay);
            }
        }

        public long getElapsedMillisec() {
            return SystemClock.uptimeMillis() - startMillisec;
        }
    }

    /* PRIVATE HELPER METHODS */
    private static void updateSizeChange(int width, int height,
                                         List<GameBackgroundInterface> backgrounds,
                                         List<GameSpriteInterface> sprites,
                                         float density) {
        for (GameBackgroundInterface bg : backgrounds ) {
            bg.onSizeChanged(width, height);
        }
        for (GameSpriteInterface sprite : sprites ) {
            sprite.onSizeChanged(width, height, density);
        }
    }

    private static void drawBackgrounds(Canvas canvas, List<GameBackgroundInterface> backgrounds) {
        for (GameBackgroundInterface bg : backgrounds ) {
            bg.draw(canvas);
        }
    }

    private static void drawSprites(Canvas canvas, List<GameSpriteInterface> sprites) {
        for (GameSpriteInterface sprite : sprites ) {
            sprite.draw(canvas);
        }
    }

    private static void updateBackgrounds(List<GameBackgroundInterface> backgrounds, long millisec) {
        for (GameBackgroundInterface bg : backgrounds ) {
            bg.update();
        }
    }

    private static void updateSprites(List<GameSpriteInterface> sprites, long millisec
            , float density) {
        for (GameSpriteInterface sprite : sprites ) {
            sprite.update(millisec, density);
        }
    }
}

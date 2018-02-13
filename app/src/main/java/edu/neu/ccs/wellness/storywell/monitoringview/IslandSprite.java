package edu.neu.ccs.wellness.storywell.monitoringview;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import edu.neu.ccs.wellness.storywell.interfaces.GameSpriteInterface;
import edu.neu.ccs.wellness.utils.WellnessGraphics;

/**
 * Created by hermansaksono on 2/8/18.
 */

public class IslandSprite implements GameSpriteInterface {

    /* STATIC VARIABLES */
    private static final float leftPaddingRatio = 0.2f;
    private static final float rightPaddingRatio = 0.2f;

    /* PRIVATE VARIABLES */
    private Bitmap bitmap;
    private float posX = 0;
    private float posY = 0;
    private int width = 100;
    private int height = 100;
    private float pivotX;
    private float pivotY;
    private String text;
    private Paint textPaint;
    private int textOffsetX;
    private float posXRatio;
    private float posYRatio;
    private float scaleRatio;

    /* CONSTRUCTOR */
    public IslandSprite (Resources res, int drawableId, String text, Paint paint,
                         float posXRatio, float posYRatio, float scaleRatio) {
        Drawable drawable = res.getDrawable(drawableId);
        this.bitmap = WellnessGraphics.drawableToBitmap(drawable);
        this.posXRatio = posXRatio;
        this.posYRatio = posYRatio;
        this.scaleRatio = scaleRatio;

        this.text = text;
        this.textPaint = paint;
        this.textOffsetX = getTextOffset(this.text, this.textPaint, this.width);
    }

    /* PUBLIC METHODS */
    @Override
    public void onSizeChanged(int width, int height) {
        this.posX = width * this.posXRatio;
        this.posY = height * this.posYRatio;
        this.width = (int) (width * scaleRatio * (1 - leftPaddingRatio - rightPaddingRatio));
        this.height = this.width;
        this.pivotX = this.width / 2;
        this.pivotY = this.height;
        this.bitmap = Bitmap.createScaledBitmap(this.bitmap, this.width , this.height, true);
    }

    @Override
    public float getPositionX() { return this.posX; }

    @Override
    public void setPositionX(float posX) { this.posX = posX; }

    @Override
    public float getPositionY() { return this.posY; }

    @Override
    public void setPositionY(float posY) { this.posY = posY; }

    @Override
    public float getAngularRotation() { /* DO NOTHING */ return 0; }

    @Override
    public void setAngularRotation(float degree) { /* DO NOTHING */ }

    @Override
    public void draw(Canvas canvas) {
        float drawPosX, drawPosY;

        drawPosX = this.posX - this.pivotX;
        drawPosY = this.posY - this.pivotY;
        canvas.drawBitmap(this.bitmap, drawPosX, drawPosY, null);

        drawPosX = this.posX - this.textOffsetX;
        drawPosY = this.posY - (int) (this.height * 0.375);
        canvas.drawText(this.text, drawPosX, drawPosY, this.textPaint);
    }

    @Override
    public void update(long millisec, float density) {
        // DO NOTHING
    }

    /* PRIVATE STATIC HELPER FUNCTION */
    private static int getTextOffset(String text, Paint paint, int width) {
        float textWidth = paint.measureText(text);
        //return (int)((width-textWidth)/2f) - (int)(paint.getTextSize());
        return (int) (textWidth/2f);
    }
}

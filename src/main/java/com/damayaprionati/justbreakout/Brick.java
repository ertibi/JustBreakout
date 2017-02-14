package com.damayaprionati.justbreakout;

/**
 * Created by rothb on 7/18/2016.
 */
import android.graphics.RectF;

public class Brick {

    private RectF rect;

    private boolean isVisible;

    public float density;

    public int color;

    public Brick(int row, int column, float width, float height, int inColor, float inDensity){

        density = inDensity;
        color = inColor;

        //this is the logic to determine if we draw a block or not
        if(color != 0) {
            isVisible = true;
        }
        else{
            isVisible = false;
        }

        float padding = dpToPix(3);

        rect = new RectF(column * width + padding, row * height + padding,
                column * width + width - padding, row * height + height - padding);
    }

    //convert dp to pixels and vice versa, this is to maintain scaling
    //on different sized screens and stuff. lotsa magic
    public float dpToPix(float dp){
        float pix = dp * (density);
        return pix;
    }

    public float pixToDp(float pix){
        float dp = pix / (density);
        return dp;
    }

    public void setColor(int inColor){
        color = inColor;
    }

    public RectF getRect(){
        return this.rect;
    }

    public void setInvisible(){
        isVisible = false;
    }

    public boolean getVisibility(){
        return isVisible;
    }
}
package com.damayaprionati.justbreakout;

/**
 * Created by rothb on 7/18/2016.
 */
import android.graphics.RectF;
import java.util.Random;

public class Ball {
    RectF rect;
    float xVelocity;
    float yVelocity;
    float ballWidth;
    float ballHeight;
    private float x;
    private float y;
    public float density;
    public int level;

    public Ball(float screenX, float screenY, float inDensity, int inLevel){

        //set level
        level = inLevel;

        //set density value
        density = inDensity;

        //initialize the speeds
        xVelocity = dpToPix(startingXVelocity());
        yVelocity = - dpToPix(setYVelocity(level)); //use 900 as max speed 375 as start

        //initialize the width and height
        ballWidth = dpToPix(6);
        ballHeight = dpToPix(6);

        //get starting values for ball coords
        x = (screenX/2) - (ballWidth/2);
        y = screenY - dpToPix(113) - (float)(ballHeight * 1.5);

        //make the rect
        rect = new RectF(x, y, x + ballWidth, y + ballHeight);
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

    //get rekt
    public RectF getRect(){
        return rect;
    }

    public float getWidth(){
        return ballWidth;
    }

    public float getHeight(){
        return ballHeight;
    }

    public float getYVelocity(){
        return yVelocity;
    }

    public void setXVelocity(float paddleSpeed){
        if (xVelocity + paddleSpeed < 900 && xVelocity + paddleSpeed > -900){
            xVelocity = xVelocity + (paddleSpeed/2);
        }
    }

    public float startingXVelocity(){
        Random rand = new Random();
        int num = rand.nextInt(150) - 75;
        return (float)num;
    }

    public float setYVelocity(int inLevel){
        float num = 30 * inLevel + 500; //500 base
        if (num <= 700){
            return num;
        }
        else{
            return 700;
        }
    }

    //update ball position
    public void update(long fps){
        rect.left = rect.left + (xVelocity / fps);
        rect.top = rect.top + (yVelocity / fps);
        rect.right = rect.left + ballWidth;
        rect.bottom = rect.top + ballHeight;
    }

    public void reverseYVelocity(){
        yVelocity = - yVelocity;
    }

    public void reverseXVelocity(){
        xVelocity = - xVelocity;
    }

    public void clearObstacleY(float y){
        rect.bottom = y;
        rect.top = y - ballHeight;
    }

    public void clearObstacleX(float x){
        rect.left = x;
        rect.right = x + ballWidth;
    }

    public void reset(int inLevel){
        level = inLevel;

        rect.left = x;
        rect.top = y;
        rect.right = x + ballWidth;
        rect.bottom = y + ballHeight;
        xVelocity = dpToPix(startingXVelocity());
        yVelocity = - dpToPix(setYVelocity(level));
    }

}

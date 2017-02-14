package com.damayaprionati.justbreakout; /**
 * Created by rothb on 7/18/2016.
 */

import android.graphics.RectF;

public class Paddle {

    //object holding our four points
    private RectF rect;

    //for figuring out pixel density and stuff
    public float density;

    public float screenX;
    public float screenY;

    private float width;
    private float height;

    //far left coord
    private float x;
    //top coord
    private float y;

    //pixels per second speed of the paddle
    public float paddleSpeed;

    //distance between the center of paddle
    //and the touch event
    public float dist;

    //constants for directions the paddle can move
    public final int STOPPED = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    //is the paddle moving? initialize it stopped
    private int paddleMoving = STOPPED;

    //constructor
    public Paddle(float inX, float inY, float inDensity){
        //density of the screen
        density = inDensity;
        screenX = inX;
        screenY = inY;

        //dimensions
        width = dpToPix(56);
        height = dpToPix(10);

        //start at center screen with room at bottom for a finger
        x = (screenX / 2) - (width / 2);
        y = screenY  - dpToPix(113);

        rect = new RectF(x, y, x + width, y + height);
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

    public void reset(){
        rect.left = x = (screenX / 2) - (width / 2);
        rect.top = screenY - dpToPix(113);
        rect.right = rect.left + width;
        rect.bottom = rect.top + height;
    }
    //public getter for the paddle's center X value
    public float getCenterX(){
        return x + (width / 2);
    }

    //getter for top coord
    public float getTop(){
        return y;
    }

    //getter for bottom coord
    public float getBottom(){
        return y + height;
    }

    //getter for the paddle coords
    public RectF getRect(){
        return rect;
    }

    //setter for movement state
    public void setMovementState(int state){
        paddleMoving = state;
    }

    //this returns the movement speed of the paddle
    public float getMovementSpeed(){
        if (paddleMoving == STOPPED){
            return 0;
        }
        else if (paddleMoving == RIGHT){
            return paddleSpeed;
        }
        else{
            return -paddleSpeed;
        }
    }

    //called from gameView, updates the paddle position
    public void update(long fps, float touchXCoord){

        //set up the way the paddle moves here
        //we're using the touchxcoord value given by
        //the event controller
        if (touchXCoord > getCenterX()){
            //first we get the distance from touch event to
            //the center of the paddle.  then using a linear
            //equation to calculate the speed of the paddle
            //we determine how far it should move relative to fps
            dist = touchXCoord - getCenterX();
            paddleSpeed = dpToPix((float)(7.5 * dist));
            x = x + paddleSpeed / fps;
            setMovementState(RIGHT);
        }
        else if (touchXCoord < getCenterX()){
            //same as above just different direction
            dist = getCenterX() - touchXCoord;
            paddleSpeed = dpToPix((float)(7.5 * dist));
            x = x - paddleSpeed / fps;
            setMovementState(LEFT);
        }
        else{
            setMovementState(STOPPED);
        }

        rect.left = x;
        rect.right = x + width;
    }

}
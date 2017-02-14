package com.damayaprionati.justbreakout;

/**
 * Created by rothb on 7/28/2016.
 */
public class ScoreBoard {

    public int score;
    public int lives;
    public int level;
    public int highScore;

    public ScoreBoard(){

        score = 0;
        lives = 3;
        level = 1; //make sure this is at 1 it might be wrong from testing

    }

    public void setHighScore(int high){
        highScore = high;
    }

    public int raiseScore(int add){
        score = score + add;
        checkHighScore(score);
        return score;
    }

    public void checkHighScore(int current){
        if (current > highScore){
            highScore = current;
        }
    }

    public void raiseLives(){
        lives++;
    }

    public int loseLives(){
        lives--;
        return lives;
    }

    public int nextLevel(){
        level++;
        return level;
    }

    public void resetScoreBoard() {
        score = 0;
        lives = 3;
        level = 1;

    }

    public int getScore(){
        return score;
    }

    public int getLives(){
        return lives;
    }

    public int getLevel(){
        return level;
    }

    public int getHighScore(){
        return highScore;
    }
}

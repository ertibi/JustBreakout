package com.damayaprionati.justbreakout;

import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.IOException;



public class MainActivity extends AppCompatActivity{

    //initialize the gameview that will hold
    //basically the whole game
    BreakoutView breakoutView;
    ScoreBoard scoreBoard;
    private AdView adView;
    private AdRequest.Builder adRequestBuilder;
    private View decorView;
    private int uiOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this block hides the status bar (top)
        //and the navigation bar (bottom) it also allows
        //content to be shown underneath those two spots
        decorView = getWindow().getDecorView();
        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION //show content under
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //show content under
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; //keeps them hidden

        decorView.setSystemUiVisibility(uiOptions); //call method that sets flags above

        //when the focus changes in the system this should rehide the status
        //and navigation bars. this is directly from android developers website
        //its a bit had wavey on my end
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener(){
            @Override
            public void onSystemUiVisibilityChange(int visibility){
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0){
                    decorView.setSystemUiVisibility(uiOptions);
                }
                else{
                    decorView.setSystemUiVisibility(uiOptions);
                }
            }
        });

        scoreBoard = new ScoreBoard();

        // Restore preferences
        SharedPreferences settings = getPreferences(0);
        int highScore = settings.getInt("highScore", 0);

        scoreBoard.setHighScore(highScore);

        //make the game veiw
        breakoutView = new BreakoutView(this, scoreBoard);

        // Add the AdView to the view hierarchy. The view will have no size
        // until the ad is loaded.
        RelativeLayout layout = new RelativeLayout(this);

        // Create an ad.
        adView = new AdView(this);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId("##################################");

        // Create an ad request.
        adRequestBuilder = new AdRequest.Builder();

        layout.addView(breakoutView);
        layout.addView(adView);

        // Start loading the ad in the background.
        adView.loadAd(adRequestBuilder.build());

        //start ad hidden
        adView.setVisibility(View.INVISIBLE);

        setContentView(layout);

    }

    public void showAd(){
        adView.loadAd(adRequestBuilder.build());
        adView.setVisibility(View.VISIBLE);
    }

    //inner class implements runnable so we can override the run method
    //and create a new thread
    class BreakoutView extends SurfaceView implements Runnable {

        //new thread
        Thread gameThread = null;

        //this is for using paint and draw
        //in a thread
        SurfaceHolder ourHolder;


        volatile boolean playing;

        //start the game paused
        boolean paused;
        boolean wonLevel;
        boolean lostLevel;
        boolean justStarted;
        boolean justWon;
        boolean justLost;

        Canvas canvas;
        Paint paint;

        // track frame rate
        long fps;

        // current time frame is drawn
        private long timeThisFrame;

        // screen size in pixels
        float screenX;
        float screenY;

        float density;

        //make the paddle object
        Paddle paddle;
        float touchXCoord;

        //make the ball object
        Ball ball;

        //make scoreboard
        ScoreBoard scoreBoard;

        //make all the levels
        Levels levels;

        //make the brick array
        Brick[][] bricks = new Brick[13][10];
        int numBricks = 0;
        int brokenBricks;

        //score ints
        int lostScore;
        int scoreMulti;
        int numBounces;

        //sound objects and variables
        SoundPool soundPool;
        int hitBrickID = -1;
        int hitOtherID = -1;
        int loseLifeID = -1;

        //constructor
        public BreakoutView(Context context, ScoreBoard board) {
            //set up the surfaceveiw
            super(context);

            //grab scoreboard from main activity
            scoreBoard = board;

            //initialize the holder and paint
            ourHolder = getHolder();
            paint = new Paint();

            //display object to pull screen specs
            Display display = getWindowManager().getDefaultDisplay();
            //get the resolution into a point object
            Point size = new Point();
            display.getRealSize(size); //get the full, real size of the screen

            //this get the dots per inch for the screen, we use it to calculate
            //the dp of the screen to help with maintaining continuity across
            //different screen sizes.
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            density = ((float)metrics.densityDpi)/DisplayMetrics.DENSITY_DEFAULT;

            //set screen size and initialize touchXCoord
            screenX = size.x;
            screenY = size.y;
            touchXCoord = screenX / 2;

            //set booleans
            paused = true;
            wonLevel = false;
            lostLevel = false;
            justStarted = true;
            justLost = false;
            justWon = false;

            //set ints
            scoreMulti = 0;
            numBounces = 0;


            paddle = new Paddle(screenX, screenY, density);

            //make the ball
            ball = new Ball(screenX, screenY, density, scoreBoard.getLevel());

            //load all the levels
            levels = new Levels();

            //this is a depreciated method of calling soundpool, will try to
            //find a fix in a new version
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            try {
                //these are required for loading the sounds
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                //load sounds in memory
                descriptor = assetManager.openFd("hitBrick.wav");
                hitBrickID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("hitOther.wav");
                hitOtherID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.wav");
                loseLifeID = soundPool.load(descriptor, 0);

            } catch (IOException e) {
                //if the sound loading fails throw error
                Log.e("error", "failed to load sound files");
            }
            //initialize the game
            createBricksAndRestart();
        }

        public void createBricksAndRestart() {

            //start ball position
            ball.reset(scoreBoard.getLevel());
            paddle.reset();

            float brickWidth = screenX / 10;
            float brickHeight = screenY / 40;

            int color;

            //this builds the bricks
            numBricks = 0;
            brokenBricks = 0;
            for (int column = 0; column < 10; column++) {
                for (int row = 0; row < 13; row++) {
                    //we iterate through the 2d array to make the bricks
                    //we make all the bricks but only pass color = 1 to
                    // the ones that we want shown.
                    color = levels.getBrick(row, column, scoreBoard.getLevel());
                    bricks[row][column] = new Brick(row, column, brickWidth, brickHeight, color, density);
                    if(color != 0) {
                        numBricks = numBricks + (color); //we only add to numbricks if the brick is drawn
                    }
                }
            }

            if(scoreBoard.getLevel() >= 1){ //fix this
                levels.makeRandomLevel();
            }
        }

        @Override
        public void run() {
            while (playing) {
                //get current time
                long startFrameTime = System.currentTimeMillis();
                //if we aren't paused then update
                if (!paused) {

                    update();
                }
                //draw after we update
                draw();
                //calculate fps to figure out animations
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }
            }
        }

        //convert dp to pixels and vice versa, this is to maintain scaling
        //on different sized screens and stuff. lotsa magic
        public float dpToPix(float dp){
            return dp * (density);
        }

        //all powerful update
        public void update() {

            //call update on paddle
            paddle.update(fps, touchXCoord);

            //call update on ball
            ball.update(fps);

            //this checks for ball collision w/ brick
            for (int column = 0; column < 10; column++) {
                for(int row = 0; row < 13; row++) {
                    if (bricks[row][column].getVisibility()) {
                        if (RectF.intersects(bricks[row][column].getRect(), ball.getRect())) {
                            //if the brick is visible we check if the ball is intersecting
                            //if it is we check the color of the brick, if its blue we break
                            //it if not we reduce its color by one
                            if(bricks[row][column].color == 1) {
                                bricks[row][column].setInvisible();
                            }
                            else{
                                bricks[row][column].setColor(bricks[row][column].color - 1);
                            }
                            ball.reverseYVelocity();
                            scoreBoard.raiseScore(1 + scoreMulti);
                            brokenBricks++;
                            soundPool.play(hitBrickID, 1, 1, 0, 0, 1);
                        }
                    } else {
                        if (brokenBricks == numBricks && brokenBricks != 0) {
                            wonLevel = true;
                            brokenBricks = 0;
                            numBricks = 0;
                        }
                    }
                }
            }

            //ball collision with paddle. if ball goes below top of paddle we check
            //if its going the right direction or not
            if (ball.getRect().bottom > paddle.getTop() && ball.getYVelocity() > 0){
                if(ball.getRect().top < (paddle.getBottom() + (ball.getHeight()/2))){
                    if(checkIntersection(ball.getRect(), paddle.getRect())){
                        ball.setXVelocity(paddle.getMovementSpeed());
                        ball.reverseYVelocity();
                        ball.clearObstacleY(paddle.getTop() - dpToPix(2));
                        numBounces++;
                        if(numBounces == 5){
                            scoreMulti++;
                            numBounces = 0;
                        }
                        soundPool.play(hitOtherID, 1, 1, 0, 0,1);
                    }
                }
            }

            //ball collision with bottom of screen
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - dpToPix(2));

                //pause so player can catch a breath
                paused = true;
                ball.reset(scoreBoard.getLevel());
                paddle.reset();

                //we lose a life if this happens
                scoreBoard.loseLives();
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                scoreMulti = 0;
                numBounces = 0;

                //end game and restart if the game ends
                if (scoreBoard.getLives() == 0) {
                    paused = true;
                    lostLevel = true;
                    lostScore = scoreBoard.getScore();
                    scoreBoard.resetScoreBoard();
                    // a lot of magic stuff. this forces the UI thread
                    // to call the view change because otherwise we throw an error
                    //the view change we call here is to serve an ad on win/loss screens
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showAd();
                        }
                    });
                    createBricksAndRestart();
                }
            }
            //ball collision with top of screen
            if (ball.getRect().top < (screenY / 50) * 3) {
                ball.reverseYVelocity();
                ball.clearObstacleY(((screenY / 50) * 3) + dpToPix(2) + ball.getHeight());
                soundPool.play(hitOtherID, 1, 1, 0, 0, 1);
            }
            //ball collision left wall
            if (ball.getRect().left < 0) {
                ball.reverseXVelocity();
                ball.clearObstacleX(dpToPix(2));
                soundPool.play(hitOtherID, 1, 1, 0, 0, 1);
            }
            //ball collision right wall
            if (ball.getRect().right > screenX - dpToPix(4)) {
                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - ball.getWidth() - dpToPix(4));
                soundPool.play(hitOtherID, 1, 1, 0, 0, 1);
            }

            //WIN!!
            if (wonLevel) {
                paused = true;
                scoreBoard.nextLevel();
                if(scoreBoard.getLevel() % 3 == 0){
                    scoreBoard.raiseLives();
                } //see loss condition above for explanation how this works
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAd();
                    }
                });
                createBricksAndRestart();
            }
        }



        //this is the draw
        public void draw() {

            //check for valid surface
            if (ourHolder.getSurface().isValid()) {
                //lock the canvas so we can draw
                canvas = ourHolder.lockCanvas();

                //background
                canvas.drawColor(Color.argb(255, 0, 0, 0));

                //brush color
                paint.setColor(Color.argb(255, 255, 255, 255));

                //draw paddle first
                canvas.drawRect(paddle.getRect(), paint);

                //draw ball
                canvas.drawRect(ball.getRect(), paint);

                //draw visible bricks
                for (int column = 0; column < 10; column++) {
                    for (int row = 0; row < 13; row++)
                    if (bricks[row][column].getVisibility()) {
                        if(bricks[row][column].color == 1){
                            paint.setColor(Color.argb(255, 48, 225, 234));
                        }
                        else if(bricks[row][column].color == 2){
                            paint.setColor(Color.argb(255, 69, 255, 48));
                        }
                        else{
                            paint.setColor(Color.argb(255, 255, 255, 48));
                        }

                        canvas.drawRect(bricks[row][column].getRect(), paint);
                    }
                }

                //brush color again again
                paint.setColor(Color.argb(255, 255, 255, 255));

                //draw scoreboard
                paint.setTextSize(dpToPix(15));
                canvas.drawText("Score: " + scoreBoard.getScore(), dpToPix(4), dpToPix(19), paint);
                canvas.drawText("Lives: " + scoreBoard.getLives(), screenX - dpToPix(70), dpToPix(19), paint);
                canvas.drawText("High: " + scoreBoard.getHighScore(), dpToPix(4), dpToPix(40), paint);

                paint.setTextSize(dpToPix(30));
                canvas.drawText("Level: " + scoreBoard.getLevel(),screenX/2 - dpToPix(60), dpToPix(27), paint);

                //start screen stuff
                if (justStarted){
                    canvas.drawText("High Score: " + scoreBoard.getHighScore(),
                            screenX/2 - dpToPix(100), dpToPix(350), paint);
                    canvas.drawText("Good Luck!",  screenX/2 - dpToPix(80), dpToPix(400), paint);
                }

                //if player won then win screen
                if (wonLevel || justWon) { // fix this
                    paint.setTextSize(dpToPix(34));
                    canvas.drawText("Level: " + scoreBoard.getLevel(),
                            screenX/2 - dpToPix(65), dpToPix(300), paint);
                    canvas.drawText("Score: " + scoreBoard.getScore(),
                            screenX/2 - dpToPix(70), dpToPix(350), paint);
                    canvas.drawText("Lives: " + scoreBoard.getLives(),
                            screenX/2 - dpToPix(65), dpToPix(400), paint);
                    wonLevel = false;
                    justWon = true;
                }

                //if player lost then loss screen
                if (lostLevel || justLost) {
                    canvas.drawText("Game Over!", screenX/2 - dpToPix(80), dpToPix(350), paint);
                    if(lostScore == scoreBoard.getHighScore()){
                        canvas.drawText("New High Score: " + scoreBoard.getHighScore(),
                                screenX/2 - dpToPix(130), dpToPix(400), paint);
                    }
                    else{
                        canvas.drawText("Final Score: " + lostScore,
                                screenX/2 - dpToPix(100), dpToPix(400), paint);
                    }
                    lostLevel = false;
                    justLost = true;
                }

                //post the updated canvas
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        //TEST THIS

        //check intersection of ball and other rectF objects
        public boolean checkIntersection(RectF ballRect, RectF other){
            //check that the left side of ball is inside the right side of
            //and if right of ball is inside left of paddle
            return ballRect.left < other.right && ballRect.right > other.left;
        }

        //if the activity is paused stop the thread
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        //if activity is resumed restart the thread
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        //override onTouchEvent from the surfaceView
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                //initial screen touch
                case MotionEvent.ACTION_DOWN:
                    if (paused) {
                        paused = false;
                    }
                    if (justStarted){
                        justStarted = false;
                    }
                    if (justWon){ // this is only between drawing and here to keep
                        justWon = false; //win screen updated
                        //adView.setVisibility(View.INVISIBLE);
                    }
                    if (justLost){
                        justLost = false;
                        //adView.setVisibility(View.INVISIBLE);
                    }
                    if (adView.getVisibility() == VISIBLE){
                        adView.setVisibility(View.INVISIBLE);
                    }

                    touchXCoord = motionEvent.getRawX(); //gets exact x coord at time of update
                    break;

                //the player moves their finger around
                case MotionEvent.ACTION_MOVE:
                    touchXCoord = motionEvent.getRawX();

                //touch removes
                case MotionEvent.ACTION_UP:
                    touchXCoord = motionEvent.getRawX();
                    break;
            }
            return true;
        }
    }
    //end breakoutView class

    //The methods below are called by the system when changes are made to
    //system focus

    //called on start
    @Override
    protected void onResume() {
        super.onResume();

        //this should ensure continuity of our ui options using the
        //settings we made in onCreate in main activity.
        decorView.setSystemUiVisibility(uiOptions);

        //call gameView reusme method
        breakoutView.resume();
    }

    //called when player quits
    @Override
    protected void onPause() {
        super.onPause();

        //call gameView pause method
        breakoutView.pause();
    }

    @Override
    protected void onStop(){
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("highScore", scoreBoard.getHighScore());

        //TEST THIS

        // Commit the edits!
        editor.apply();

        scoreBoard.resetScoreBoard();
    }
}
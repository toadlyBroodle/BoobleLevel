package ca.toadlybroodle.levelboobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

// (.Y.) Behold the mighty and majestic boobs simulation stuff (.Y.)
public class BoobsSimView extends View implements SensorEventListener {

    final String TAG = "BoobsSimView";

    private static final float sGravity = 9.81f; // m/(s^2)
    // Diameter of boobs in meters
    private static final float sFriction = 0.3f;
    private static float sElasticity = 20000000f;
    private final float sBoobDiameter;
    private final String fu = "I'm flattered that you would try to hack my code...all the best:)";
    private int viewWidth;
    private int viewHeight;
    private float mLastDeltaT;
    private float mMetersToPixelsX;
    private float mMetersToPixelsY;
    private int boobWidth;
    private int boobHeight;
    private float mXOrigin;
    private float mYOrigin;
    private int firstTouchX = 0;
    private int firstTouchY = 0;
    private float firstTouchBoobPosX = 0;
    private float firstTouchTopBoobPosY = 0;
    private float firstTouchBottomBoobPosY = 0;
    private float mSensorX;
    private float mSensorY;
    private long mLastT;
    private long mSensorTimeStamp;
    private long mCpuTimeStamp;
    private Bitmap bodyBackground;
    private Bitmap boobLeft;
    private Bitmap boobRight;
    private Sensor mAccelerometer;
    private RackSystem mRackSystem;
    private Paint paint;
    private Paint paintText;
    private Paint bikini;

    // Constructor
    @SuppressWarnings("deprecation")
    public BoobsSimView(Context context, AttributeSet attributes) {
        super(context, attributes);

        Log.d(TAG, fu);
        Log.d(TAG, TAG + "'s constructor initiated");

        mAccelerometer = BoobleLevelActivity.mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Convert display metrics to units of pixels
        float mXDpi = BoobleLevelActivity.metrics.xdpi;
        float mYDpi = BoobleLevelActivity.metrics.ydpi;
        mMetersToPixelsX = mXDpi / 0.0254f;
        mMetersToPixelsY = mYDpi / 0.0254f;

        // get boob scaling factor in meters for various calculations
        sBoobDiameter = (BoobleLevelActivity.mDisplay.getWidth() / 1.8f)
                / mMetersToPixelsX - (16 / mMetersToPixelsY);

        // Instantiate rack system
        mRackSystem = new RackSystem();

    }

    public void startBoobsSimView() {
        Log.d(TAG, "start" + TAG + "() initiated");

        // setup paints
        paint = new Paint();
        paintText = new Paint();
        bikini = new Paint();
        setupPaints();

        // Register sensor listener
        BoobleLevelActivity.mSensorManager.registerListener(this,
                mAccelerometer, SensorManager.SENSOR_DELAY_UI);

        boobWidth = (int) (sBoobDiameter * mMetersToPixelsX);
        boobHeight = (int) (sBoobDiameter * mMetersToPixelsY);

        // load correct bitmaps for current skin preference
        // and scale boobs accordingly
        switch (BoobleLevelActivity.prefsSkin) {
            case 0:
                Bitmap boobSusy = BitmapFactory.decodeResource(getResources(),
                        R.drawable.susy_boob);
                boobLeft = Bitmap.createScaledBitmap(boobSusy, boobWidth, boobWidth,
                        true);
                boobRight = Bitmap.createScaledBitmap(boobSusy, boobWidth, boobWidth,
                        true);
                break;
            case 1:
                Bitmap boobBarb = BitmapFactory.decodeResource(getResources(),
                        R.drawable.barb_boob);
                boobLeft = Bitmap.createScaledBitmap(boobBarb, boobWidth, boobWidth,
                        true);
                boobRight = Bitmap.createScaledBitmap(boobBarb, boobWidth, boobWidth,
                        true);
                break;
            case 2:

                Bitmap boobLeftBrittany = BitmapFactory.decodeResource(getResources(),
                        R.drawable.brittany_boob_left);

//                float scalingFactor = boobWidth / boobLeftBrittany.getWidth();
//                boobHeight = (int) (boobLeftBrittany.getHeight() * scalingFactor);

                boobLeft = Bitmap.createScaledBitmap(boobLeftBrittany, boobWidth, boobWidth,
                        true);
                Bitmap boobRightBrittany = BitmapFactory.decodeResource(getResources(),
                        R.drawable.brittany_boob_right);
                boobRight = Bitmap.createScaledBitmap(boobRightBrittany, boobWidth, boobWidth,
                        true);

                break;
            case 3:

                // make Max's boobs a little smaller, TODO screws everything up
//                boobWidth = (int) (boobWidth*0.9);
//                boobHeight = (int) (boobHeight*0.9);

                Bitmap boobLeftMax = BitmapFactory.decodeResource(getResources(),
                        R.drawable.max_left_boob);
                boobLeft = Bitmap.createScaledBitmap(boobLeftMax, boobWidth, boobWidth,
                        true);
                Bitmap boobRightMax = BitmapFactory.decodeResource(getResources(),
                        R.drawable.max_right_boob);
                boobRight = Bitmap.createScaledBitmap(boobRightMax, boobWidth, boobWidth,
                        true);

                break;
            case 5:

                break;
            default:
        }

        // try resetting background image in case skin preference changed
        try {
            loadUpBackground();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void stopBoobsSimView() {

        Log.d(TAG, "stop" + TAG + "() initiated");

        // Unregister sensor listener
        BoobleLevelActivity.mSensorManager.unregisterListener(this);

    }

    // Single boob class
    class Boob {

        private String mName; // "top" or "bottom"
        private float mPosX;
        private float mPosY;
        private float mAccelX;
        private float mAccelY;
        private float mAccelTotX;
        private float mAccelTotY;
        private float mLastPosX;
        private float mLastPosY;
        private float mOneMinusFriction;
        private float mIndOriginX;
        private float mIndOriginY;
        private float mIndHoriBound;
        private float mIndVertiBound;
        private float elastForceX;
        private float elastForceY;
        private boolean mTouched = false;

        Boob(String name) {
            mName = name;

            // Make each boob slightly different by randomizing its
            // coefficient of friction
            final float r = ((float) Math.random() - 0.5f) * 0.1f;
            mOneMinusFriction = 1.0f - sFriction + r;

        }

        public void setIndOrigin() {
            // Set origin of boob and assign appropriate confining radius
            if (mName.equals("top")) {
                mIndOriginX = 0;
                mIndOriginY = -(viewHeight - boobLeft.getHeight()) / 4f;
                mIndHoriBound = ((viewWidth / mMetersToPixelsX - sBoobDiameter) / 2f);
                mIndVertiBound = -((viewHeight / mMetersToPixelsX - sBoobDiameter) / 2f);

                // Log.d(TAG, mName + "mIndOrigins set");
            } else { // mName will be "bottom"
                mIndOriginX = 0;
                mIndOriginY = (viewHeight - boobRight.getHeight()) / 4f;
                mIndHoriBound = ((viewWidth / mMetersToPixelsX - sBoobDiameter) / 2f);
                mIndVertiBound = ((viewHeight / mMetersToPixelsX - sBoobDiameter) / 2f);

                Log.d(TAG, mName + "mIndOrigins set");
            }

        }

        public void computePhysics(float sx, float sy, float dT, float dTC) {

            if (!mTouched) {

                final float m = 1000.0f; // mass of the boob
                // Force of gravity applied to the boob
                final float gx = -sx * m;
                final float gy = -sy * m;

                // Acceleration of the boob
                final float invm = 1.0f / m;
                final float ax = gx * invm;
                final float ay = gy * invm;

				/*
                 * Time-corrected Verlet integration: The position Verlet
				 * integrator is defined as x(t+�t) = x(t) + x(t) - x(t-�t) +
				 * a(t)�t�2 However, the above equation doesn't handle variable
				 * �t very well, a time-corrected version is needed: x(t+�t) =
				 * x(t) + (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2 We also add
				 * a simple friction term (f) to the equation: x(t+�t) = x(t) +
				 * (1-f) * (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2
				 */
                final float dTdT = dT * dT;
                final float x = mPosX + mOneMinusFriction * dTC
                        * (mPosX - mLastPosX) + mAccelTotX * dTdT;
                final float y = mPosY + mOneMinusFriction * dTC
                        * (mPosY - mLastPosY) + mAccelTotY * dTdT;
                mLastPosX = mPosX;
                mLastPosY = mPosY;
                mPosX = x;
                mPosY = y;

                // Factor force of elasticity towards boob's origin into
                // acceleration

                elastForceX = (float) Math.pow((mPosX), 3d) * sElasticity;

                if (mName.equals("top")) {
                    // -top so correct to make positive
                    final float yOrig = -mIndOriginY / mMetersToPixelsY;
                    final float yCurrPos = mPosY; // +top


                    elastForceY = (float) Math.pow((yOrig - yCurrPos), 3d)
                            * sElasticity;

                    // elastForceY neg if above origin and pos if below origin

                } else { // mName will be "bottom"
                    final float yOrig = mIndOriginY / mMetersToPixelsY; // +bottom
                    // -bottom so correct to make positive
                    final float yCurrPos = -mPosY; // -bottom

                    elastForceY = (float) Math.pow((yCurrPos - yOrig), 3d)
                            * sElasticity;
                }
                // elastForceY neg if above origin and pos if below origin


                mAccelX = ax;
                mAccelY = ay;

                final float cap = 15;
                // put cap on elastic force
                if (elastForceX > cap) {
                    elastForceX = cap;
                } else if (elastForceX < -cap) {
                    elastForceX = -cap;
                }
                if (elastForceY > cap) {
                    elastForceY = cap;
                } else if (elastForceY < -cap) {
                    elastForceY = -cap;
                }

                mAccelTotX = mAccelX - elastForceX;
                mAccelTotY = mAccelY + elastForceY;

                // Log.d(TAG, "computePhysics done");
            }
        }

        public void resolveCollisionWithBounds() {

            // Restrict boobs to their respective quadrants
            final float xmax = mIndHoriBound;
            final float ymax = mIndVertiBound; // - for "top", + for "bottom"
            final float x = mPosX;
            final float y = mPosY; // + for "top", - for "bottom"
            if (x > xmax) {
                mPosX = xmax;
            } else if (x < -xmax) {
                mPosX = -xmax;
            }
            if (mName.equals("top")) {
                if (y < 0) {
                    mPosY = 0;
                } else if (-y < ymax) {
                    mPosY = -ymax;
                }
            } else { // mName will be "bottom"
                if (-y > ymax) {
                    mPosY = -ymax;
                } else if (y > 0) {
                    mPosY = 0;
                }

            }
        }

    }

    class RackSystem {
        static final int NUM_BOOBS = 2;
        public Boob rack[] = new Boob[NUM_BOOBS];

        RackSystem() {

            // Initially our particles have no speed or acceleration
            rack[0] = new Boob("top");
            rack[1] = new Boob("bottom");
        }

        /*
         * Update the position of each particle in the system using the Verlet
         * integrator.
         */
        private void updatePositions(float sx, float sy, long timestamp) {

            if (mLastT != 0) {
                final float dT = (float) (timestamp - mLastT) * (1.0f / 1000000000.0f);
                if (mLastDeltaT != 0) {
                    final float dTC = dT / mLastDeltaT;
                    final int count = rack.length;
                    for (int i = 0; i < count; i++) {
                        Boob boob = rack[i];
                        boob.computePhysics(sx, sy, dT, dTC);
                    }
                }
                mLastDeltaT = dT;
            }
            mLastT = timestamp;
        }

        /*
         * Performs one iteration of the simulation. First updating the position
         * of all the particles and resolving the constraints and collisions.
         */
        public void update(float sx, float sy, long now) {
            // update the system's positions
            updatePositions(sx, sy, now);

            // We do no more than a limited number of iterations
            final int NUM_MAX_ITERATIONS = 4;

			/*
             * Resolve collisions, each particle is tested against every other
			 * particle for collision. If a collision is detected the particle
			 * is moved away using a virtual spring of infinite stiffness.
			 */
            boolean more = true;
            final int count = rack.length;
            for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++) {
                more = false;
                for (int i = 0; i < count; i++) {
                    Boob curr = rack[i];
                    for (int j = 0; j < count; j++) {
                        Boob ball = rack[j];
                        float dx = ball.mPosX - curr.mPosX;
                        float dy = ball.mPosY - curr.mPosY;
                        float dd = dx * dx + dy * dy;
                        // Check for collisions
                        if (dd <= sBoobDiameter * sBoobDiameter) {
                            /*
                             * add a little bit of entropy, after nothing is
							 * perfect in the universe.
							 */
                            dx += ((float) Math.random() - 0.5f) * 0.0001f;
                            dy += ((float) Math.random() - 0.5f) * 0.0001f;
                            dd = dx * dx + dy * dy;
                            // simulate the spring
                            final float d = (float) Math.sqrt(dd);
                            final float c = (0.5f * (sBoobDiameter - d)) / d;
                            curr.mPosX -= dx * c;
                            curr.mPosY -= dy * c;
                            ball.mPosX += dx * c;
                            ball.mPosY += dy * c;
                            more = true;
                        }
                    }

                    // and finally make sure boob stays in it's quadrant
                    curr.resolveCollisionWithBounds();
                }
            }
        }

        public String getName(int zeroOrOne) {
            return rack[zeroOrOne].mName;
        }

        public int getNumBoobs() {
            return NUM_BOOBS;
        }

        public int getIndOriginX(int zeroOrOne) {
            return (int) rack[zeroOrOne].mIndOriginX;
        }

        public int getIndOriginY(int zeroOrOne) {
            return (int) rack[zeroOrOne].mIndOriginY;
        }

        public float getPosX(int i) {
            return rack[i].mPosX;
        }

        public float getPosY(int i) {
            return rack[i].mPosY;
        }

        public void setPosX(int x, int firstX, float firstBoobX, int i) {

            // calculate difference between first touch and last touch and move
            // boob accordingly
            final float diffX = (x - firstX) / mMetersToPixelsX; // meters
            final float diffBoobX = rack[i].mPosX - firstBoobX;
            final float moveDistX = diffX - diffBoobX;

            rack[i].mPosX = rack[i].mPosX + moveDistX;

        }

        public void setPosY(int y, int firstY, float firstBoobY, int i) {

            // account for difference in touch point and boob's originY, and
            // move boob accordingly, while accounting for +top and -bottom
            final float diffY = (y - firstY) / mMetersToPixelsY; // meters
            final float diffBoobY = firstBoobY - rack[i].mPosY;
            final float moveDistY = diffBoobY - diffY;

            rack[i].mPosY = rack[i].mPosY + moveDistY;
        }

        public boolean isTouched(int i) {
            return rack[i].mTouched;
        }

        public void setTouched(boolean touched, int i) {
            rack[i].mTouched = touched;
            // if (touched) Log.d(TAG, rack[i].mName + " touched!");
            // if (!touched) Log.d(TAG, rack[i].mName + " not touched.");
        }

        public void handleActionDown(int eventX, int eventY, int i) {

            // if eventX and eventY are inside bitmap then set boob to touched
            final float posX = mXOrigin + rack[i].mPosX * mMetersToPixelsX;
            // + top, -bottom
            final float posY = mYOrigin - rack[i].mPosY * mMetersToPixelsY;

            if ((eventX >= posX) && (eventX <= (posX + boobLeft.getHeight()))) {
                if ((eventY >= posY)
                        && (eventY <= (posY + boobLeft.getWidth()))) {
                    // boob touched
                    mRackSystem.setTouched(true, i);
                } else {
                    mRackSystem.setTouched(false, i);
                }
            } else {
                mRackSystem.setTouched(false, i);
            }

        }

        // Used for testing purposes
/*        public float getAccelX(int zeroOrOne) {
            return rack[zeroOrOne].mAccelX;
        }

        public float getAccelY(int zeroOrOne) {
            return rack[zeroOrOne].mAccelY;
        }

        public float getElastForceX(int zeroOrOne) {
            return rack[zeroOrOne].elastForceX;
        }

        public float getElastForceY(int zeroOrOne) {
            return rack[zeroOrOne].elastForceY;
        }*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // set view width and height for use elsewhere
        viewWidth = w;
        viewHeight = h;

        // compute the origin of the screen relative to the origin of
        // the boob bitmap
        mXOrigin = (w - boobLeft.getWidth()) * 0.5f;
        mYOrigin = (h - boobLeft.getHeight()) * 0.5f;

        // load background images
        loadUpBackground();

        // Set individual origins of boobs in rack system
        final int count = mRackSystem.getNumBoobs();
        for (int j = 0; j < count; j++) {
            mRackSystem.rack[j].setIndOrigin();

        }

        Log.d(TAG, TAG + "'s onSizeChanged completed");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //listen for touched boobs
        final int count = mRackSystem.getNumBoobs();
        for (int i = 0; i < count; i++) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                // delegate event handling to mRackSystem
                mRackSystem.handleActionDown((int) event.getX(),
                        (int) event.getY(), i);

                // record first coordinates
                firstTouchX = (int) event.getX();
                firstTouchY = (int) event.getY();
                firstTouchBoobPosX = mRackSystem.getPosX(i);
                if (mRackSystem.getName(i).equals("top")) {
                    firstTouchTopBoobPosY = mRackSystem.getPosY(i);
                } else { // mName will be "bottom"
                    firstTouchBottomBoobPosY = mRackSystem.getPosY(i);
                }

                Log.d(TAG,
                        "Coords: x=" + event.getX() + ",y=" + event.getY());

            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // if boob is touched
                // Log.d(TAG, "Action move called");
                if (mRackSystem.isTouched(i)) {
                    // boob is being dragged, get touchPoints and send to
                    // mRackSystem for processing
                    mRackSystem.setPosX((int) event.getX(), firstTouchX,
                            firstTouchBoobPosX, i);

                    if (mRackSystem.getName(i).equals("top")) {
                        mRackSystem.setPosY((int) event.getY(),
                                firstTouchY, firstTouchTopBoobPosY, i);
                    } else { // mName will be "bottom"
                        mRackSystem.setPosY((int) event.getY(),
                                firstTouchY, firstTouchBottomBoobPosY, i);
                    }
                    // Log.d(TAG, "Action move finished");

                }
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // touch is released
                // Log.d(TAG, "Action up called");
                if (mRackSystem.isTouched(i)) {
                    mRackSystem.setTouched(false, i);
                    Log.d(TAG, "Action up finished");
                }
            }
        }
        // return super.onTouchEvent(event); // this doesn't work??
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Log.d(TAG, "onSensorChanged() initiated");

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        /*
		 * Record the accelerometer data, the event's timestamp and the current
		 * time. The latter is needed so we can calculate the "present" time
		 * during rendering. In this application, we need to take into account
		 * how the screen is rotated with respect to the sensors (which always
		 * return data in a coordinate space aligned to with the screen in its
		 * native orientation).
		 */

        switch (BoobleLevelActivity.mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                mSensorX = event.values[0];
                mSensorY = event.values[1];
                break;
            case Surface.ROTATION_90:
                mSensorX = -event.values[1];
                mSensorY = event.values[0];
                break;
            case Surface.ROTATION_180:
                mSensorX = -event.values[0];
                mSensorY = -event.values[1];
                break;
            case Surface.ROTATION_270:
                mSensorX = event.values[1];
                mSensorY = -event.values[0];
                break;
        }

        mSensorTimeStamp = event.timestamp;
        mCpuTimeStamp = System.nanoTime();

    }

    //    @SuppressLint("DefaultLocale")
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Log.d(TAG, "onDraw() initiated");

        // Draw the background according to skin preference selected
        canvas.drawBitmap(bodyBackground, 0, 0, null);

		/*
		 * compute the new position of boobs, based on accelerometer data and
		 * present time.
		 */
        Context context = getContext();
        final RackSystem rack = mRackSystem;
        final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
        float sx = mSensorX;
        float sy = mSensorY;
        String xEquals = context.getString(R.string.x_equals);
        String yEquals = context.getString(R.string.y_equals);

        rack.update(sx, sy, now);

        final float xc = mXOrigin;
        final float yc = mYOrigin;
        final float xs = mMetersToPixelsX;
        final float ys = mMetersToPixelsY;
        final float halfBoobDiam = sBoobDiameter * mMetersToPixelsX / 2f;
        final int count = rack.getNumBoobs();
        for (int j = 0; j < count; j++) {
			/*
			 * We transform the canvas so that the coordinate system matches the
			 * sensors coordinate system with the origin in the center of the
			 * screen and the unit is the meter.
			 */
            float x = xc + rack.getPosX(j) * xs;
            float y = yc - rack.getPosY(j) * ys;
            // draw boobs and bikini staps according to selected skin
            int k;
            float oppX;
            float oppY;
            float shoulderY;
            float shoulderX;
            float boobTopLeftY;
            float boobTopLeftX;
            float boobTopRightY;
            float boobTopRightX;
            float boobBottomLeftY;
            float boobBottomLeftX;
            float boobBottomRightY;
            float boobBottomRightX;
            switch (BoobleLevelActivity.prefsSkin) {
                case 0:
                    canvas.drawBitmap(boobLeft, x, y, null);
                    break;
                case 1:

                    // draw shoulder straps
                    if (mRackSystem.getName(j).equals("top"))
                        shoulderY = bodyBackground.getHeight() * 0.39f;
                    else
                        shoulderY = bodyBackground.getHeight() * 0.612f;
                    canvas.drawLine(bodyBackground.getWidth() * 0.95f, shoulderY, x
                                    + boobLeft.getWidth() - 4f, y + halfBoobDiam - 7f,
                            bikini);

                    if (j == 0) {
                        k = 1;
                        oppX = mRackSystem.getPosX(k) * mMetersToPixelsX;
                        oppY = -mRackSystem.getPosY(k) * mMetersToPixelsY;
                        canvas.drawLine(x + boobLeft.getHeight() * 0.21f, y
                                        + boobLeft.getWidth() * 0.904f,
                                oppX + boobLeft.getHeight() * 0.665f, oppY
                                        + boobLeft.getWidth() * 1.096f, bikini);

                        canvas.drawBitmap(boobLeft, x, y, null);
                    } else canvas.drawBitmap(boobLeft, x, y, null);
                    break;
                case 2:

                    // draw shoulder straps
                    if (mRackSystem.getName(j).equals("top")) {
                        shoulderY = bodyBackground.getHeight() * 0.39f;
                        shoulderX = bodyBackground.getWidth() * 0.855f;
                        boobTopLeftY = y + halfBoobDiam - 48f;
                        boobTopLeftX = x + boobLeft.getWidth() - 8f;
                        canvas.drawLine(shoulderX, shoulderY, boobTopLeftX, boobTopLeftY, bikini);
                    } else {
                        shoulderY = bodyBackground.getHeight() * 0.612f;
                        shoulderX = bodyBackground.getWidth() * 0.87f;
                        boobTopLeftY = y + halfBoobDiam + 48f;
                        boobTopLeftX = x + boobLeft.getWidth() - 8f;
                        canvas.drawLine(shoulderX, shoulderY, boobTopLeftX, boobTopLeftY, bikini);
                    }
                    if (j == 0) {
                        k = 1;

                        // draw boob connecting strap
                        oppX = mRackSystem.getPosX(k) * mMetersToPixelsX;
                        oppY = -mRackSystem.getPosY(k) * mMetersToPixelsY;
                        canvas.drawLine(x + boobLeft.getHeight() * 0.155f,
                                y + boobLeft.getWidth() * 0.89f,
                                oppX + boobLeft.getHeight() * 0.59f,
                                oppY + boobLeft.getWidth() * 1.096f,
                                bikini);

                        canvas.drawBitmap(boobLeft, x, y, null);
                    } else canvas.drawBitmap(boobRight, x, y, null);
                    break;
                case 3:

                    // draw shoulder straps
                    if (mRackSystem.getName(j).equals("top")) {
                        shoulderY = bodyBackground.getHeight() * 0.42f;
                        shoulderX = bodyBackground.getWidth() * 0.94f;
                        boobTopLeftY = y + halfBoobDiam - 59f;
                        boobTopLeftX = x + boobLeft.getWidth() - 8f;
                        canvas.drawLine(shoulderX, shoulderY, boobTopLeftX, boobTopLeftY, bikini);
                    } else {
                        shoulderY = bodyBackground.getHeight() * 0.62f;
                        shoulderX = bodyBackground.getWidth() * 0.93f;
                        boobTopLeftY = y + halfBoobDiam + 56f;
                        boobTopLeftX = x + boobLeft.getWidth() - 8f;
                        canvas.drawLine(shoulderX, shoulderY, boobTopLeftX, boobTopLeftY, bikini);
                    }
                    if (j == 0) {
                        k = 1;

                        // draw boob connecting strap
                        oppX = mRackSystem.getPosX(k) * mMetersToPixelsX;
                        oppY = -mRackSystem.getPosY(k) * mMetersToPixelsY;
                        canvas.drawLine(x + boobLeft.getHeight() * 0.17f,
                                y + boobLeft.getWidth() * 0.92f,
                                oppX + boobLeft.getHeight() * 0.61f,
                                oppY + boobLeft.getWidth() * 1.06f,
                                bikini);

                        canvas.drawBitmap(boobLeft, x, y, null);
                    } else canvas.drawBitmap(boobRight, x, y, null);
                    break;

            }
        }

        // Draw two cross-hairs on top and bottom boob's respective origins if
        // preference box checked
        if (BoobleLevelActivity.prefsXHairs) {
            // draw xhairs
            for (int i = 0; i < count; i++) {
					/*
					 * We transform the canvas so that the coordinate system
					 * matches the sensors coordinate system with the origin in
					 * the center of the screen and the unit is the meter.
					 */
                float x = xc + rack.getIndOriginX(i) + halfBoobDiam;
                float y = yc + rack.getIndOriginY(i) + halfBoobDiam;
                canvas.drawLine(x - halfBoobDiam, y, x + halfBoobDiam, y,
                        paint);
                canvas.drawLine(x, y - halfBoobDiam, x, y + halfBoobDiam,
                        paint);
                // Log.d(TAG, "cross-hairs for loop run " + i);
            }
        }

        // draw x and y tilt angles (in degrees) if pref_angles selected
        if (BoobleLevelActivity.prefsAngles) {

            double lengthXMax = (double) sGravity;
            double lengthYMax = (double) sGravity;
            double lengthXMin = (double) -sGravity;
            double lengthYMin = (double) -sGravity;

            float mSensorXCenter = 0;
            double lengthX = sx - mSensorXCenter;
            float mSensorYCenter = 0;
            double lengthY = sy - mSensorYCenter;

            if (lengthX < lengthXMin)
                lengthX = lengthXMin;
            if (lengthX > lengthXMax)
                lengthX = lengthXMax;

            if (lengthY < lengthYMin)
                lengthY = lengthYMin;
            if (lengthY > lengthYMax)
                lengthY = lengthYMax;

            double degreesX = 0;
            double degreesY = 0;

            if (lengthX < mSensorXCenter)
                degreesX = -Math.toDegrees(Math.asin(lengthX / lengthXMin));
            if (lengthX >= mSensorXCenter)
                degreesX = Math.toDegrees(Math.asin(lengthX / lengthXMax));

            if (lengthY < mSensorYCenter)
                degreesY = -Math.toDegrees(Math.asin(lengthY / lengthYMin));
            if (lengthY >= mSensorYCenter)
                degreesY = Math.toDegrees(Math.asin(lengthY / lengthYMax));

            // draw x/y in degrees
            canvas.drawText(
                    xEquals + Integer.toString((int) Math.ceil(degreesX))
                            + (char) 0x00B0, 20, 40, paintText);
            canvas.drawText(
                    yEquals + Integer.toString((int) Math.ceil(degreesY))
                            + (char) 0x00B0, 20, 70, paintText);

            // // temp display lengths for testing
            // if (lengthX < mSensorXCenter)
            // canvas.drawText(lengthX + "/" + lengthXMin, 20, 100,
            // paintText);
            // if (lengthX > mSensorXCenter)
            // canvas.drawText(lengthX + "/" + lengthXMax, 20, 100,
            // paintText);
            //
            // if (lengthY < mSensorYCenter)
            // canvas.drawText(lengthY + "/" + lengthYMin, 20, 130,
            // paintText);
            // if (lengthY > mSensorYCenter)
            // canvas.drawText(lengthY + "/" + lengthYMax, 20, 130,
            // paintText);

        }

        // ________________________________________________________ //
        // temp draw various boob attributes to screen for testing
        //for (int i = 0; i < count; i++) {
			/*
			 * We transform the canvas so that the coordinate system matches the
			 * sensors coordinate system with the origin in the center of the
			 * screen and the unit is the meter.
			 */

        // final float x = xc + rack.getPosX(i) * xs;
        // final float y = yc - rack.getPosY(i) * ys;
        // canvas.drawLine(x - 30, y, x + 30, y, paint);
        // canvas.drawLine(x, y - 30, x, y + 30, paint);

        // temp print out mPosX/Y, mAccelX/Y, elastForceX/Y
        // final String textPosX = Float.toString(rack.getPosX(i));
        // final String textPosY = Float.toString(rack.getPosY(i));
        // canvas.drawText(textPosX, x, y, paint);
        // canvas.drawText(textPosY, x, y + 20, paint);

        // final String textAccelX = Float.toString(rack.getAccelX(i));
        // final String textAccelY = Float.toString(rack.getAccelY(i));
        // final String textElastForceX = Float.toString(rack
        // .getElastForceX(i));
        // final String textElastForceY = Float.toString(rack
        // .getElastForceY(i));
        // canvas.drawText(textAccelX, x + 5, y, paint);
        // canvas.drawText(textElastForceX, x + 5, y + 20, paint);
        // canvas.drawText(textAccelY, x + 10, y + 40, paint);
        // canvas.drawText(textElastForceY, x + 10, y + 60, paint);
        //
        // // Log.d(TAG, "cross-hairs for loop run " + i);
        // }

        // // temp draw horiz and verti bounds
        // final float ex = xc + mHorizontalBound * xs;
        // final float wi = yc - mVerticalBound * ys;
        // canvas.drawLine(ex, wi, ex, yc, paint);
        // canvas.drawLine(xc, yc, ex, yc, paint);

        // // temp circle at origins and hori/verti bounds
        // canvas.drawCircle(xc, yc, 10, paint);
        // for (int i = 0; i < count; i++) {
        // canvas.drawCircle(xc + rack.getIndOriginX(i),
        // yc + rack.getIndOriginY(i), mConfineRadius
        // * mMetersToPixelsX, paint);
        //
        // final float xb = rack.getIndHoriBound(i) * xs;
        // final float yb = rack.getIndVertiBound(i) * ys;
        // canvas.drawLine(xc + xb, yc + yb, xc - xb, yc, paint);
        //
        // Log.d(TAG, "temp circle and bounds drawn");
        //}
        // _________________________________________________________ //

        // and make sure to redraw asap
        invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void setupPaints() {

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1f);
        paint.setStyle(Paint.Style.STROKE);
        paintText.setColor(Color.BLACK);
        paintText.setStrokeWidth(2f);
        paintText.setTextSize(30);
        paintText.setStyle(Paint.Style.FILL_AND_STROKE);

        switch (BoobleLevelActivity.prefsSkin) {
            case 0:
                break;
            case 1:
                bikini.setColor(Color.BLACK);
                bikini.setStrokeWidth(10f);
                bikini.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
            case 2:
                bikini.setColor(0xFFC2D787);
                bikini.setStrokeWidth(14f);
                bikini.setStyle(Paint.Style.FILL_AND_STROKE);

                // make boobs a little saggier
                sElasticity = 18000000f;
                break;
            case 3:
                bikini.setColor(0xFF330000);
                bikini.setStrokeWidth(10f);
                bikini.setStyle(Paint.Style.FILL_AND_STROKE);

                sElasticity = 18000000f;
                break;
        }
    }

    public void loadUpBackground() {
        // load background according to selected skin preference and
        // use screen width and height to appropriately resize background
        Options opts = new Options();
        opts.inDither = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        switch (BoobleLevelActivity.prefsSkin) {
            case 0:
                Bitmap backgroundSusy = BitmapFactory.decodeResource(getResources(),
                        R.drawable.susy_body, opts);
                bodyBackground = Bitmap.createScaledBitmap(backgroundSusy, viewWidth, viewHeight, true);
                break;
            case 1:
                Bitmap backgroundBarb = BitmapFactory.decodeResource(getResources(),
                        R.drawable.barb_body, opts);
                bodyBackground = Bitmap.createScaledBitmap(backgroundBarb, viewWidth, viewHeight, true);
                break;
            case 2:

                Bitmap backgroundBrittany = BitmapFactory.decodeResource(getResources(),
                        R.drawable.brittany_body, opts);
                bodyBackground = Bitmap.createScaledBitmap(backgroundBrittany, viewWidth, viewHeight, true);
                break;
            case 3:

                Bitmap backgroundMax = BitmapFactory.decodeResource(getResources(),
                        R.drawable.max_body, opts);
                bodyBackground = Bitmap.createScaledBitmap(backgroundMax, viewWidth, viewHeight, true);
                break;


        }
    }
}
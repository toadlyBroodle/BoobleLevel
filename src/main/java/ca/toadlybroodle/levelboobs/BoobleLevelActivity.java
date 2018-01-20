package ca.toadlybroodle.levelboobs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import ca.toadlybroodle.levelboobs.util.IabHelper;
import ca.toadlybroodle.levelboobs.util.IabResult;
import ca.toadlybroodle.levelboobs.util.Inventory;
import ca.toadlybroodle.levelboobs.util.Purchase;

public class BoobleLevelActivity extends Activity implements
        OnSharedPreferenceChangeListener {

    final String TAG = "BoobleLevelActivity";

    static SensorManager mSensorManager;
    static WindowManager mWindowManager;
    static Display mDisplay;
    static DisplayMetrics metrics = new DisplayMetrics();
    private BoobsSimView mBoobsSimView;
    // Does the user have the premium upgrade?
    static boolean mHasCompSkinsPack = false;
    static boolean mHasInventoryQueryCompleted = false;
    static final String SKU_ALL_SKINS = "all_skins";
    private static final int RC_REQUEST = 10001;
    // application's public key TODO ADD AND OBFUSCATE THIS!!!
    String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlr4v07qKYeXkPX4XOYSU15tBrh8OIPBUVABjiD1FPSm5kn9XLZsgaWx20mDLHzUy0FO/39bSGlBsdirAD42/TQ1o2rJi6ebPaH89hoj8auQrRkJlSPKnxDHMFAJcPd9gthpQ3wjCoe8O0YlsdrfucdkkBkbFDlTleJQTo96de1I3GR09kLu78gRnAytAgczh4XURfqN2m+bwk/uZExnYD2Jyq6wnDMVY6wO4PhHFKYUPewzMiRiwyF4GYA+doABUWhOlT0fWGA/mbOv+x1jCQgfJhSHJlwjoRTYwYBOtSFnls6WQ6MdLJOY+wN5QihnEkQ3/45imXRhmiKrYiNWURQIDAQAB";
    IabHelper mHelper;
    public static SharedPreferences prefs;
    static boolean prefsXHairs;
    static boolean prefsAngles;
    static int prefsSkin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, TAG + "'s onCreate() initiated");

        // Get instance of SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get an instance of the WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();

        // Get display metrics
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // initiate synchronous communication with google play app
//        mHelper = new IabHelper(this, base64EncodedPublicKey);
//        // enable debug logging TODO set this to false for production
//        mHelper.enableDebugLogging(true);
//        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
//            public void onIabSetupFinished(IabResult result) {
//                Log.d(TAG, "Setup finished.");
//
//                if (!result.isSuccess()) {
//                    // Oh noes, there was a problem.
//                    complain("Problem setting up in-app billing: " + result);
//
//                    // skip inventory check
//                    mHasInventoryQueryCompleted = true;
//                    return;
//                }
//
//                // Have we been disposed of in the meantime? If so, quit.
//                if (mHelper == null) return;
//
//                // IAB is fully set up. Now, let's get an inventory of stuff we own.
//                Log.d(TAG, "Setup successful. Querying inventory.");
//                mHelper.queryInventoryAsync(mGotInventoryListener);
//            }
//        });

        // Instantiate our layout and set it as the activity's content
        setContentView(R.layout.activity_booblelevel);

        // get instance of BoobsSimView
        mBoobsSimView = (BoobsSimView) findViewById(R.id.boobs_sim_view);

    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            mHasInventoryQueryCompleted = true;

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase compSkinsPackPurchase = inventory.getPurchase(SKU_ALL_SKINS);
            mHasCompSkinsPack = (compSkinsPackPurchase != null && verifyDeveloperPayload(compSkinsPackPurchase));
            Log.d(TAG, "User has " + (mHasCompSkinsPack ? "SKU_ALL_SKINS" : "not purchased SKU_ALL_SKINS"));

            testForUnauthorizedSkinUsage();
            setWaitScreen(false);

            Log.d(TAG, "Initial inventory query finished");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, TAG + "'s onResume() initiated");

        // Make sure screen stays on while animation running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // get preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsXHairs = prefs.getBoolean("prefs_xhairs", true);
        prefsAngles = prefs.getBoolean("prefs_angles", true);
        final String tempPrefsSkin = prefs.getString("prefs_skins_list", "1");
        prefsSkin = Integer.parseInt(tempPrefsSkin);

        testForUnauthorizedSkinUsage();

        // Start boobs simulation
        mBoobsSimView.startBoobsSimView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop boobs simulation
        mBoobsSimView.stopBoobsSimView();

    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "BoobleLevelActivity.onStop() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // mHelper may have encounter errors along the way and be null?
        try {
            // unbind from In-App-Billing service
            if (mHelper != null) mHelper.dispose();
            mHelper = null;

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // make sure back button doesn't navigate back through stack to preferences activity
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);

    }

    // Create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Open Preferences Activity when menu settings button clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, TAG + "'s onOptionsItemSelected() initiated");

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {

    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    public void testForUnauthorizedSkinUsage() {
        //TODO update any applicable info based on purchased skins

        // if done querying inventory and user has not bought the skins pack
        // and has an unavailable skin selected then display the sales pitch screen
        if (mHasInventoryQueryCompleted && !mHasCompSkinsPack && prefsSkin > 1) {
            setSalesPitchScreen(true);
        }
    }

    public void onNegBuyButtonClicked(View view) {

        // send back to preferences menu
        startActivity(new Intent(this, PrefsActivity.class));
    }

    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        // mHelper may have encounter errors along the way and be null
        try {
            mHelper.launchPurchaseFlow(this, SKU_ALL_SKINS, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_ALL_SKINS)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert(getString(R.string.purchase_thank_you));
                mHasCompSkinsPack = true;
                testForUnauthorizedSkinUsage();
                setWaitScreen(false);
            }
        }
    };

    void complain(String message) {
        Log.e(TAG, "**** BoobleLevel Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton(R.string.neutral_button, null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        findViewById(R.id.boobs_sim_view).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    //enables or disables the "sales pitch" screen
    void setSalesPitchScreen(boolean set) {
        findViewById(R.id.boobs_sim_view).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.salesPitchLayout).setVisibility(set ? View.VISIBLE : View.GONE);

    }
}
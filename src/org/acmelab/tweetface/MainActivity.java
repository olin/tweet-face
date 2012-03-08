package org.acmelab.tweetface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Tweet Face
 * (C) Mark L. Chang, 2012
 * Clipart provided by http://openclipart.org/detail/37063/visage-by-antoine-37063
 *
 * Tweets your face. Seriously.
 */
public class MainActivity extends FragmentActivity {
    public static final String TAG = "TWEETFACE";
    public static final int TWEET_MAX_LENGTH = 119;

    private Camera camera;
    private SurfaceView preview;
    private boolean inPreview = false;
    private boolean takingPicture = false;

    private EditText tweetText;
    private TextView tweetLength;
    private Button tweetButton;

    private static Twitter twitter;
    private static RequestToken requestToken;
    private static SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Util.makeDirs();

        mSharedPreferences = getSharedPreferences(Const.PREFERENCE_NAME, MODE_PRIVATE);
        tweetText = (EditText)findViewById(R.id.tweetText);
        tweetLength = (TextView)findViewById(R.id.tweetLength);
        tweetButton = (Button)findViewById(R.id.tweetButton);

        tweetText.addTextChangedListener(tweetTextWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();

        handleOAuth();

        // invalidate Menu for Android 3.0
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            invalidateOptionsMenu();

        // start camera
        preview = (SurfaceView) findViewById(R.id.cameraPreview);
        SurfaceHolder previewHolder = preview.getHolder();
        previewHolder.removeCallback(surfaceCallback);
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if( inPreview ){
            camera.stopPreview();
        }

        if (camera != null) {
            camera.release();
            camera = null;
            inPreview = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem loginItem = menu.findItem(R.id.menu_login);
        MenuItem logoutItem = menu.findItem(R.id.menu_logout);

        if(isConnected()) {
            loginItem.setEnabled(false);
            loginItem.setVisible(false);
            logoutItem.setEnabled(true);
            logoutItem.setVisible(true);
        } else {
            logoutItem.setEnabled(false);
            logoutItem.setVisible(false);
            loginItem.setEnabled(true);
            loginItem.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_login:
                askOAuth();
                return true;
            case R.id.menu_logout:
                disconnectTwitter();
                Toast.makeText(this, "Twitter disconnected", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * check if the account is authorized
     * @return
     */
    private boolean isConnected() {
        return mSharedPreferences.getString(Const.PREF_KEY_TOKEN, null) != null;
    }

    public void clickTweet(View view) {
        if (isConnected()) {
            if (Util.isOnline(this)) {
                if (tweetText.length() <= TWEET_MAX_LENGTH && tweetText.length() > 0) {
                    if (camera != null && inPreview && !takingPicture) {
                        takingPicture = true;
                        camera.autoFocus(autoFocusCallback);
                    }
                } else if(tweetText.length() == 0) {
                    Toast.makeText(this, "Tweet something at least!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Tweet too long!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show();
            }

        } else {
            // trigger authentication
            askOAuth();
        }
    }

    private void handleOAuth() {
        /**
         * Handle OAuth Callback
         */
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Const.CALLBACK_URL)) {
            String verifier = uri.getQueryParameter(Const.IEXTRA_OAUTH_VERIFIER);
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putString(Const.PREF_KEY_TOKEN, accessToken.getToken());
                e.putString(Const.PREF_KEY_SECRET, accessToken.getTokenSecret());
                e.commit();

                Toast.makeText(this, "Twitter connected!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void askOAuth() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(Const.CONSUMER_KEY);
        configurationBuilder.setOAuthConsumerSecret(Const.CONSUMER_SECRET);
        Configuration configuration = configurationBuilder.build();
        twitter = new TwitterFactory(configuration).getInstance();

        try {
            requestToken = twitter.getOAuthRequestToken(Const.CALLBACK_URL);
            Toast.makeText(this, "Please authorize this app!", Toast.LENGTH_LONG).show();
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove Token, Secret from preferences
     */
    private void disconnectTwitter() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Const.PREF_KEY_TOKEN);
        editor.remove(Const.PREF_KEY_SECRET);

        editor.commit();
    }
    
    private int getFrontCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1; // No front-facing camera found
    }


    private void initCamera() {
        try {
            if (camera == null) {
                int index = getFrontCameraId();
                if (index == -1) {
                    Toast.makeText(this, "No front camera! Bailing.", Toast.LENGTH_SHORT).show();
                } else {
                    camera = Camera.open(index);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot initialize camera", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result=size;
            }
            else {
                int resultArea=result.width * result.height;
                int newArea=size.width * size.height;

                if (newArea < resultArea) {
                    result=size;
                }
            }
        }

        return(result);
    }

    private final TextWatcher tweetTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            tweetLength.setText(String.valueOf(TWEET_MAX_LENGTH - charSequence.length()));
            if(charSequence.length() > TWEET_MAX_LENGTH) {
                tweetButton.setEnabled(false);
            } else {
                tweetButton.setEnabled(true);
            }
        }

        public void afterTextChanged(Editable editable) {

        }
    };

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean b, Camera camera) {
            if (takingPicture) {
                camera.takePicture(null, null, photoCallback);
                inPreview = false;
                takingPicture = true;
            }
        }
    };

    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePhotoTweetTask().execute(data);
            Toast.makeText(MainActivity.this, "Tweeting in the background", Toast.LENGTH_SHORT).show();
            camera.startPreview();
            inPreview = true;
        }
    };


    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {

        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // stop preview before making changes
            if (camera != null) {
                try {
                    camera.stopPreview();
                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                }

                Camera.Parameters parameters = camera.getParameters();

                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                Camera.Size pictureSize = getSmallestPictureSize(parameters);
                parameters.setPictureSize(pictureSize.width, pictureSize.height);
                parameters.setPictureFormat(ImageFormat.JPEG);
                parameters.setRotation(270);
                camera.setParameters(parameters);

                try {
                    camera.setPreviewDisplay(holder);
                } catch (IOException exception) {
                    camera.release();
                    camera = null;
                }

                camera.startPreview();
                camera.autoFocus(autoFocusCallback);
                inPreview = true;
            } else {
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (inPreview) {
                camera.stopPreview();
            }

            if (camera != null) {
                camera.release();
                camera = null;
                inPreview = false;
            }
        }
    };

    private class SavePhotoTweetTask extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean result) {
            takingPicture = false;
            
            if(result) {
                tweetText.setText("");
            } else {
                Toast.makeText(MainActivity.this, "Error Tweeting. Try again in a few moments.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(byte[]... jpeg) {
            // save file
            File photo = Util.getTempFile();
            if (photo.exists()) photo.delete();
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e(TAG, "Exception in photoCallback", e);
                return false;
            }

            // set up twitter
            String oauthAccessToken = mSharedPreferences.getString(Const.PREF_KEY_TOKEN, "");
            String oAuthAccessTokenSecret = mSharedPreferences.getString(Const.PREF_KEY_SECRET, "");

            ConfigurationBuilder confbuilder = new ConfigurationBuilder();
            Configuration conf = confbuilder
                    .setOAuthConsumerKey(Const.CONSUMER_KEY)
                    .setOAuthConsumerSecret(Const.CONSUMER_SECRET)
                    .setOAuthAccessToken(oauthAccessToken)
                    .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
                    .build();

            // tweet text with media
            try {
                String tweet = tweetText.getText().toString();
                Twitter twitter = new TwitterFactory(conf).getInstance();
                StatusUpdate statusUpdate = new StatusUpdate(tweet);
                statusUpdate.setMedia(Util.getTempFile());
                twitter.updateStatus(statusUpdate);
            } catch (TwitterException e) {
                Log.e(TAG, "Error updating Twitter status", e);
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }

}

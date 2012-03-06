package org.acmelab.tweetface;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    public static final String TAG = "TWEETFACE";

    private Camera camera;
    private SurfaceView preview;
    private boolean inPreview = false;
    private boolean takingPicture = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.e(TAG, "CREATE");
        Util.makeDirs();

    }

    @Override
    protected void onResume() {
        super.onResume();

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

    public void clickTweet(View view) {
        Log.e(TAG, "Tweeting and taking picture");

        if (Util.isOnline(this)) {
            if (camera != null && inPreview && !takingPicture) {
                Log.e(TAG, "Taking picture");
                takingPicture = true;
                camera.autoFocus(autoFocusCallback);
            }
        } else {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show();
        }
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
        Log.i(TAG, "Initializing camera");
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


    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean b, Camera camera) {
            if (takingPicture) {
                Log.e(TAG, "Now, actually taking picture");
                camera.takePicture(null, null, photoCallback);
                inPreview = false;
                takingPicture = false;
            }
        }
    };

    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "Saving photo");
            new SavePhotoTask().execute(data);
            finish();
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
            Log.i(TAG, "Surface getting destroyed");
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

    private class SavePhotoTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... jpeg) {
            File photo = Util.getTempFile();

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return null;
        }
    }

}

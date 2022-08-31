package com.hyq.hm.test.timewarpscan;

import static com.hyq.hm.test.timewarpscan.speed.MODE_FAST;
import static com.hyq.hm.test.timewarpscan.speed.MODE_NORMAL;
import static com.hyq.hm.test.timewarpscan.speed.MODE_SLOW;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.hyq.hm.test.timewarpscan.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Objects;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int FRONT_CAMERA = 1;
    private static final int BACK_CAMERA = 0;
    public int ANIMATION_DURATION;
    private static final int SPEED_SLOW = 4;
    private static final int SPEED_NORMAL = 8;
    private static final int SPEED_FAST = 16;
    private static final String MODE_HORIZONTAL = "horizontal";
    private static final String MODE_VERTICAL = "vertical";
    private speed speedMode = MODE_NORMAL;

    private static final int STANDARD_WIDTH = 1280;
    float previewWidth;
//    private static final int STANDARD_HEIGHT = 2560;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        clickListeners();
    }

    private void initSizes() {
        previewWidth = binding.cameraView.getScreenWidth();

        Log.d("TAG", "initSizes: " + previewWidth);

        if (previewWidth != STANDARD_WIDTH){
            if (previewWidth > STANDARD_WIDTH){
                float percentage = (STANDARD_WIDTH / previewWidth) * 100;
//                percentage = 100 - percentage;

                float s1 = (binding.cameraView.getSpeed() * percentage) / 100;
                float s2 = s1 / 2.85f;
                Log.d("TAG", "initSizes: " + s1 + " " + s2);
                binding.cameraView.setSpeed((binding.cameraView.getSpeed() + s1) - s2);
            }
            else{
                float percentage = (previewWidth / STANDARD_WIDTH) * 100;
//                percentage = 100 - percentage;

                float s1 = (binding.cameraView.getSpeed() * percentage) / 100;
                float s2 = s1 / 2.85f;
                Log.d("TAG", "initSizes: " + s1 + " " + s2);
                binding.cameraView.setSpeed((binding.cameraView.getSpeed() + s1) - s2);
            }
        }

//        if (Objects.equals(mode, MODE_VERTICAL) &&previewHeight != STANDARD_HEIGHT){
//            if (previewHeight > STANDARD_HEIGHT){
//                float percentage = (STANDARD_HEIGHT / previewHeight) * 100;
//                percentage = 100 - percentage;
//
//                float s1 = (binding.cameraView.getSpeed() * percentage) / 100;
//                float s2 = s1 / 2;
//
//                Log.d("TAG", "initSizes: " + s1 + " " + s2);
//                binding.cameraView.setSpeed(binding.cameraView.getSpeed() + s1 + s2);
//            }
//            else{
//                float percentage = (previewHeight / STANDARD_HEIGHT) * 100;
//                percentage = 100 - percentage;
//
//                float s1 = (binding.cameraView.getSpeed() * percentage) / 100;
//                float s2 = s1 / 2;
//                Log.d("TAG", "initSizes: " + s1 + " " + s2);
//                binding.cameraView.setSpeed(binding.cameraView.getSpeed() + s1 + s2);
//            }
//        }
    }

    private void clickListeners() {
        binding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOrStopScan("");
                binding.btnNormal.performClick();
            }
        });

        binding.btnSlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllInactive();
                binding.btnSlow.setTextColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
                binding.cameraView.setSpeed(SPEED_SLOW);
                speedMode = MODE_SLOW;
            }
        });

        binding.btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllInactive();
                binding.btnNormal.setTextColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
                binding.cameraView.setSpeed(SPEED_NORMAL);
                speedMode = MODE_NORMAL;
            }
        });

        binding.btnFast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllInactive();
                binding.btnFast.setTextColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
                binding.cameraView.setSpeed(SPEED_FAST);
                speedMode = MODE_FAST;
            }
        });

        binding.btnWarpHorizontal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initSizes();
                onScan(MODE_HORIZONTAL);
            }
        });

        binding.btnWarpVertical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initSizes();
                onScan(MODE_VERTICAL);
            }
        });

        binding.btnSaveToGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                takeScreenshot(root);
                ReadPixelsTask task = new ReadPixelsTask(Math.round(binding.cameraView.getScreenWidth()),
                        Math.round(binding.cameraView.getScreenHeight()), 100);
                task.execute();
            }
        });

        binding.btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCameraSwitch(view);
            }
        });
    }

    private void setAllInactive() {
        binding.btnFast.setTextColor(Color.BLACK);
        binding.btnNormal.setTextColor(Color.BLACK);
        binding.btnSlow.setTextColor(Color.BLACK);
    }

    public void onScan(String mode){
        binding.cameraView.setWarpMode(this, mode);
        startOrStopScan(mode);
    }

    private void startOrStopScan(String mode) {
        binding.cameraView.setScanVideo(!binding.cameraView.isScanVideo());
        showOrHideActions(mode);
    }

    private void showOrHideActions(String mode) {
        if(binding.cameraView.isScanVideo()){
            binding.rlActions.setVisibility(View.GONE);
            binding.btnReset.setVisibility(View.VISIBLE);
            binding.rlSpeedControlActions.setVisibility(View.GONE);

            animateLineSeparator(mode);
        }else{
            binding.verticalLineSeparator.setVisibility(View.GONE);
            binding.horizontalLineSeparator.setVisibility(View.GONE);
            binding.btnReset.setVisibility(View.GONE);
            binding.rlActions.setVisibility(View.VISIBLE);
            binding.rlSpeedControlActions.setVisibility(View.VISIBLE);
        }
    }
    ObjectAnimator animation;
    private void animateLineSeparator(String mode) {
        Log.d("TAG", "animateLineSeparator: " + binding.cameraView.getSpeed());
        if (!mode.isEmpty()) {
            int animationDuration = binding.cameraView.getAnimationWidth();
            if (speedMode == MODE_SLOW){
                animationDuration *= 2;
            }
            else if (speedMode == MODE_FAST){
                animationDuration /= 2;
            }

            ANIMATION_DURATION = animationDuration;

            Log.d("TAG", "animateLineSeparator: " + binding.cameraView.getRectangle());
            Log.d("TAG", "Animation duration: " + ANIMATION_DURATION);

            if (mode.equals(MODE_HORIZONTAL)) {
                binding.verticalLineSeparator.setVisibility(View.VISIBLE);
                animation = ObjectAnimator.ofFloat(binding.verticalLineSeparator, "translationX",
                        binding.cameraView.getRectangle().left, binding.cameraView.getRectangle().right);
            }
            else {
                binding.horizontalLineSeparator.setVisibility(View.VISIBLE);
                animation = ObjectAnimator.ofFloat(binding.horizontalLineSeparator, "translationY",
                        binding.cameraView.getRectangle().top, binding.cameraView.getRectangle().bottom);
            }

            animation.setDuration(ANIMATION_DURATION);
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
        }
    }

//    2560 x 1280
//    2592 x 1952 | 2304x1296
//    2304 x 1728

    public void onCameraSwitch(View view){
        if(binding.cameraView.isShowFrontCamera(this)){
            switchCamera(this, FRONT_CAMERA);
        }else{
            switchCamera(this, BACK_CAMERA);
        }
        binding.cameraView.destroyAll();
        recreate();
    }

    public void switchCamera(Context context, int camera) {
        SharedPreferences sharedPref = context.getSharedPreferences("camera", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("camera", camera);
        editor.apply();
    }

    private boolean hasWritePermissions() {
        return EasyPermissions.hasPermissions(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    private void requestExternalStoragePermission() {
        EasyPermissions.requestPermissions(
                this, "Please grant storage access to save file",
                121, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    private void takeScreenshot(View view) {
        // create bitmap screen capture
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        String fileName = saveToInternalStorage(bitmap);

        if (fileName != null){
            Toast.makeText(getApplicationContext(),"Saved in " + fileName, Toast.LENGTH_LONG).show();
//            openScreenshot(new File(fileName));
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        File directory = getDirectory();

        if (directory != null) {
            File myPath = getFilePath(directory);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(myPath);
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return myPath.getAbsolutePath();
        }

        return null;
    }

    private File getFilePath(File directory) {
        return new File(directory, new Date().getTime() + ".jpg");
    }

    private File getDirectory() {
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/Tws");
        } else {
            directory = new File(Environment.getExternalStorageDirectory().toString() + "/Tws");
        }

        if (!directory.exists()) {
            // Make it, if it doesn't exit
            boolean success = directory.mkdirs();
            if (!success) {
                directory = null;
            }
        }

        return directory;
    }

    private class ReadPixelsTask extends AsyncTask<Void, Integer, Long> {
        private int mWidth;
        private int mHeight;
        private int mIterations;

        /**
         * Prepare for the glReadPixels test.
         */
        public ReadPixelsTask(
                int width, int height, int iterations) {
            mWidth = width;
            mHeight = height;
            mIterations = iterations;
        }

        @Override
        protected Long doInBackground(Void... params) {
            long result;
            EglCore eglCore = null;
            OffscreenSurface surface = null;

            try {
                eglCore = new EglCore(null, 0);
                surface = new OffscreenSurface(eglCore, mWidth, mHeight);
                Log.d("TAG", "Buffer size " + mWidth + "x" + mHeight);
                result = runGfxTest(surface);
            } finally {
                if (surface != null) {
                    surface.release();
                }
                if (eglCore != null) {
                    eglCore.release();
                }
            }
            return result < 0 ? result : result / mIterations;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

        @Override
        protected void onPostExecute(Long result) {
            Log.d("TAG", "onPostExecute result=" + result);

            Resources res = getResources();
            if (result < 0) {
//                setMessage(mResultTextId, res.getString(R.string.did_not_complete));
            } else {
//                setMessage(mResultTextId, (result / 1000) +
//                        res.getString(R.string.usec_per_iteration));
            }
        }

        /**
         * Does a simple bit of rendering and then reads the pixels back.
         *
         * @return total time spent on glReadPixels()
         */
        private long runGfxTest(OffscreenSurface eglSurface) {
            long totalTime = 0;

            eglSurface.makeCurrent();

            ByteBuffer pixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
            pixelBuf.order(ByteOrder.LITTLE_ENDIAN);

            Log.d("TAG", "Running...");
            float colorMult = 1.0f / mIterations;
            for (int i = 0; i < mIterations; i++) {
                if ((i % (mIterations / 8)) == 0) {
                    publishProgress(i);
                }

                // Clear the screen to a solid color, then add a rectangle.  Change the color
                // each time.
                float r = i * colorMult;
                float g = 1.0f - r;
                float b = (r + g) / 2.0f;
                GLES20.glClearColor(r, g, b, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(mWidth / 4, mHeight / 4, mWidth / 2, mHeight / 2);
                GLES20.glClearColor(b, g, r, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

                // Try to ensure that rendering has finished.
                GLES20.glFinish();
                GLES20.glReadPixels(0, 0, 1, 1,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);

                // Time individual extraction.  Ideally we'd be timing a bunch of these calls
                // and measuring the aggregate time, but we want the isolated time, and if we
                // just read the same buffer repeatedly we might get some sort of cache effect.
                long startWhen = System.nanoTime();
                GLES20.glReadPixels(0, 0, mWidth, mHeight,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
                totalTime += System.nanoTime() - startWhen;
            }
            Log.d("TAG", "done");

            // save the last one off into a file
            long startWhen = System.nanoTime();
            try {
                File directory = getDirectory();
                File file = getFilePath(directory);
                Log.d("TAG", "runGfxTest: " + file);
                eglSurface.saveFrame(file);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            Log.d("TAG", "Saved frame in " + ((System.nanoTime() - startWhen) / 1000000) + "ms");

            return totalTime;
        }

        private File getFilePath(File directory) {
            return new File(directory, new Date().getTime() + ".jpg");
        }

        private File getDirectory() {
            File directory;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/Tws");
            } else {
                directory = new File(Environment.getExternalStorageDirectory().toString() + "/Tws");
            }

            if (!directory.exists()) {
                // Make it, if it doesn't exit
                boolean success = directory.mkdirs();
                if (!success) {
                    directory = null;
                }
            }

            return directory;
        }
    }
} enum speed {
    MODE_NORMAL,
    MODE_SLOW,
    MODE_FAST;
        }
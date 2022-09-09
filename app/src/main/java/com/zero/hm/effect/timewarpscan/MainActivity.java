package com.zero.hm.effect.timewarpscan;

import static com.zero.hm.effect.timewarpscan.speed.MODE_FAST;
import static com.zero.hm.effect.timewarpscan.speed.MODE_NORMAL;
import static com.zero.hm.effect.timewarpscan.speed.MODE_SLOW;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.zero.hm.effect.timewarpscan.databinding.ActivityMainBinding;
import com.zero.hm.effect.timewarpscan.databinding.DialogInitBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity implements Listener, EasyPermissions.PermissionCallbacks {

    private ActivityMainBinding binding;

    private static final int FRONT_CAMERA = 1;
    private static final int BACK_CAMERA = 0;

    private ObjectAnimator animation;

    private speed speedMode = MODE_NORMAL;
    private static final int SPEED_SLOW = 4;
    private static final int SPEED_NORMAL = 8;
    private static final int SPEED_FAST = 16;

    private AlertDialog dialog;

    private static final String MODE_HORIZONTAL = "horizontal";
    private static final String MODE_VERTICAL = "vertical";
    private String mode;

    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        init();
        binding.cameraView.setListener(this);
        clickListeners();
    }

    private void init() {
        binding.root.getKeepScreenOn();

        if (!isAppInitDone()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInitBinding dialogInitBinding = DialogInitBinding.inflate(getLayoutInflater());
            builder.setView(dialogInitBinding.getRoot());
            dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();

            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    binding.cameraView.setSpeed(SPEED_FAST * 8);
                    binding.btnWarpHorizontal.performClick();
                }
            }, 1000);
        }
    }

    private boolean isAppInitDone() {
        SharedPreferences sharedPref = getSharedPreferences("camera", Context.MODE_PRIVATE);
        return sharedPref.getBoolean("isInitDone", false);
    }

    public void setAppInitDone() {
        SharedPreferences sharedPref = getSharedPreferences("camera", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isInitDone", true);
        editor.apply();
    }

    public long getRearAnimationDuration() {
        SharedPreferences sharedPref = getSharedPreferences("camera", Context.MODE_PRIVATE);
        return sharedPref.getLong("rearAnimationDuration", 0);
    }

    public long getFrontAnimationDuration() {
        SharedPreferences sharedPref = getSharedPreferences("camera", Context.MODE_PRIVATE);
        return sharedPref.getLong("frontAnimationDuration", 0);
    }

    private void clickListeners() {
        binding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOrStopScan();
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
                onScan(MODE_HORIZONTAL);
            }
        });

        binding.btnWarpVertical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScan(MODE_VERTICAL);
            }
        });

        binding.btnSaveToGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("TAG", "onClick: " + hasWritePermissions());
                if (hasWritePermissions()) {
                    hideAllActions();
                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startProjection();
                        }
                    }, 1000);
                }
                else {
                    requestExternalStoragePermission();
                }
            }
        });

        binding.btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCameraSwitch();
            }
        });

        binding.cameraView.setOnTouchListener(new OnSwipeTouchListener(this){
            @Override
            public void onSwipeRight() {
                binding.btnWarpHorizontal.performClick();
            }

            @Override
            public void onSwipeBottom() {
                binding.btnWarpVertical.performClick();
            }
        });
    }

    private void hideAllActions() {
        binding.rlActions.setVisibility(View.GONE);
        binding.rlSpeedControlActions.setVisibility(View.GONE);
        binding.rlMoreActions.setVisibility(View.GONE);
    }

    private void showAllActions() {
        binding.rlMoreActions.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            startService(ScreenCaptureService.getStartIntent(this, resultCode, data, this));
        }
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        MediaProjectionManager mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        startService(ScreenCaptureService.getStopIntent(this));
    }


    private void setAllInactive() {
        binding.btnFast.setTextColor(Color.BLACK);
        binding.btnNormal.setTextColor(Color.BLACK);
        binding.btnSlow.setTextColor(Color.BLACK);
    }

    public void onScan(String mode){
        binding.cameraView.setWarpMode(this, mode);
        this.mode = mode;
        startOrStopScan();
    }

    private void startOrStopScan() {
        binding.cameraView.setScanVideo(!binding.cameraView.isScanVideo());
        showOrHideActions();
    }

    private void showOrHideActions() {
        if(binding.cameraView.isScanVideo()){
            binding.rlActions.setVisibility(View.GONE);
            binding.rlMoreActions.setVisibility(View.VISIBLE);
            binding.rlSpeedControlActions.setVisibility(View.GONE);
        }else{
            binding.verticalLineSeparator.setVisibility(View.GONE);
            binding.horizontalLineSeparator.setVisibility(View.GONE);
            binding.rlMoreActions.setVisibility(View.GONE);
            binding.rlActions.setVisibility(View.VISIBLE);
            binding.rlSpeedControlActions.setVisibility(View.VISIBLE);
        }
    }

    private void animateLineSeparator() {
        if (!mode.isEmpty()) {
            long animationDuration;
            if (binding.cameraView.isRearCameraActive(this))
                animationDuration = getRearAnimationDuration();
            else
                animationDuration = getFrontAnimationDuration();

            if (speedMode == MODE_SLOW){
                animationDuration *= (SPEED_FAST / SPEED_SLOW);
            }
            else if (speedMode == MODE_NORMAL){
                animationDuration *= (SPEED_FAST / SPEED_NORMAL);
            }

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

            animation.setDuration(animationDuration);
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
        }
    }

    @Override
    public void initDone(final int stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (stage == 1){
                    if (dialog.isShowing()) dialog.dismiss();

                    binding.btnReset.performClick();
                    binding.btnSwitch.performClick();
                }
                else if (stage == 2){
                    if (dialog.isShowing()) dialog.dismiss();

                    setAppInitDone();
                    binding.btnReset.performClick();
                    binding.btnSwitch.performClick();
                }
            }
        });
    }

    @Override
    public void startAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                if (isAppInitDone())
//                    animateLineSeparator();
            }
        });
    }

    @Override
    public void imageSavedSuccessfully(final String filePath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopProjection();
                showAllActions();
                Toast.makeText(MainActivity.this, "File saved successfully in " + filePath, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void onCameraSwitch(){
        if(binding.cameraView.isRearCameraActive(this)){
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
                100, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


} enum speed {
    MODE_NORMAL,
    MODE_SLOW,
    MODE_FAST;
        }
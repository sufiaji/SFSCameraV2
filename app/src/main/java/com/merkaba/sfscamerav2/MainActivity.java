package com.merkaba.sfscamerav2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.io.File;

import static android.os.Environment.DIRECTORY_PICTURES;

public class MainActivity extends AppCompatActivity {

//    private String ACCESS_TOKEN =
//            "aQFvPEG6hqcAAAAAAAAAATREys7x89cfhiXElv8w2kII9cBGQet9FaWuyfp47w5c";
    private CameraView mCamera;
    private int mUploadCounter;
    private int mLocalSaveCounter;
    private int mThreadFinish;
    private Button mButtonCamera;
    private TextView mTextCounter;
    private int mMaxPictures = 5;
    private Context mContext;
    private String TAG = "JANCUK";
    private String mAccessToken;
    private int mTimerTIme = 5;
    private boolean mFlash = true;
    private boolean mLocalSave = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mButtonCamera = findViewById(R.id.buttonCamera);
        mTextCounter = findViewById(R.id.counter);
        mAccessToken = retrieveAccessToken();
        getTimerTime();
        getMaxPictures();
        getUserAccount();
        getFlash();
        getLocalSavingFlag();
        mTextCounter.setText("0/" + mMaxPictures);
        mCamera = findViewById(R.id.camera);
        if(mFlash)
            mCamera.setFlash(Flash.ON);
        else
            mCamera.setFlash(Flash.OFF);
        mCamera.setLifecycleOwner(this);
        mCamera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                uploadDbx(result.getData(), mUploadCounter);
                if(mLocalSave) {
                    File pathname = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
                    String imageName = "ayam_" + System.currentTimeMillis() + ".jpg";
                    File destFile = new File(pathname, imageName);
                    result.toFile(destFile, new FileCallback() {
                        @Override
                        public void onFileReady(@Nullable File file) {
                            Toast.makeText(getApplicationContext(), "Foto ke " + mLocalSaveCounter + " tersimpan.", Toast.LENGTH_SHORT).show();
                            mLocalSaveCounter += 1;
                        }
                    });
                }
                if(mUploadCounter < mMaxPictures) {
                    mUploadCounter += 1;
                    takePictureAfterDelay(mTimerTIme);
                } else {
                    mButtonCamera.setEnabled(true);
                }
            }
        });
    }

    private String retrieveAccessToken() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "Token tidak ada.");
            return null;
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists");
            return accessToken;
        }
    }

    private void takePictureAfterDelay(int seconds) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mCamera.takePicture();
            }
        };
        handler.postDelayed(runnable, seconds*1000);
    }

    public void onTakePicture(View view) {
        mTextCounter.setText("0/" + mMaxPictures);
        mButtonCamera.setEnabled(false);
        mUploadCounter = 1;
        mLocalSaveCounter = 1;
        mThreadFinish = 0;
        mCamera.takePicture();
    }

    private void getFlash() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mFlash = prefs.getBoolean("flash", mFlash);
    }

    private void getMaxPictures() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mMaxPictures = prefs.getInt("maxpictures", mMaxPictures);
    }

    private void getLocalSavingFlag() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mLocalSave = prefs.getBoolean("islocal", mLocalSave);
    }

    public void onPicCounterButton(View view) {
        String sCurrentMax = String.valueOf(mMaxPictures);
        new LovelyTextInputDialog(this)
                .setTitle("Maksimal Foto")
                .setMessage("Maksimal foto yg diambil (skg " + sCurrentMax + " foto)")
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(text.isEmpty()) return;
                        mMaxPictures = Integer.parseInt(text);
                        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
                        prefs.edit().putInt("maxpictures", mMaxPictures).apply();
                        Toast.makeText(MainActivity.this, "Maksimal foto yg diambil adalah " + text + " foto", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void getTimerTime() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mTimerTIme = prefs.getInt("timertime", mTimerTIme);
    }

    public void onTimerButton(View view) {
        String sCurrentTime = String.valueOf(mTimerTIme);
        new LovelyTextInputDialog(this)
                .setTitle("Timer")
                .setMessage("Jarak antar pengambilan foto (skg " + sCurrentTime + " detik)" )
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(text.isEmpty()) return;
                        mTimerTIme = Integer.parseInt(text);
                        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
                        prefs.edit().putInt("timertime", mTimerTIme).apply();
                        Toast.makeText(MainActivity.this, "Jarak pengambilan foto adalah " + text + " detik", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();

    }

    public void onFlashButton(View view) {
        mFlash = !mFlash;
        if(mFlash) {
            mCamera.setFlash(Flash.ON);
            Toast.makeText(getApplicationContext(), "Flash ON", Toast.LENGTH_SHORT).show();
        }
        else {
            mCamera.setFlash(Flash.OFF);
            Toast.makeText(getApplicationContext(), "Flash OFF", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("flash", mFlash).apply();
    }

    public void onLocalButton(View view) {
        mLocalSave = !mLocalSave;
        if(mLocalSave) {
            Toast.makeText(getApplicationContext(), "Penyimpanan lokal ON", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Penyimpanan lokal OFF", Toast.LENGTH_SHORT).show();
        }
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("islocal", mLocalSave).apply();
    }

    private void uploadDbx(byte[] data, int counter) {
        new UploadFileTask(mContext, DropboxClient.getClient(mAccessToken), data, counter, new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result, int counter) {
                mThreadFinish += 1;
                Toast.makeText(mContext, "Foto ke " + Integer.toString(counter) + " sukses upload.", Toast.LENGTH_SHORT).show();
                mTextCounter.setText(Integer.toString(mThreadFinish) + "/" + mMaxPictures);
                if(mThreadFinish== mMaxPictures) ding();
            }

            @Override
            public void onError(Exception e, int counter) {
                mThreadFinish += 1;
                Toast.makeText(mContext, "Foto ke " + Integer.toString(counter) + " gagal upload.", Toast.LENGTH_SHORT).show();
                mTextCounter.setText(Integer.toString(mThreadFinish) + "/" + mMaxPictures);
                if(mThreadFinish== mMaxPictures) ding();
            }
        }).execute();
    }

    private void ding() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MediaPlayer mPlayer = MediaPlayer.create(mContext, R.raw.eventually);
                mPlayer.setLooping(false);
                mPlayer.start();
            }
        };
        handler.post(runnable);

    }

    private void uploadDbxThread(PictureResult result) {
        UploadDropbox uploadDropbox = new UploadDropbox(result.getData(),
                DropboxClient.getClient(mAccessToken));
        Thread thread = new Thread(uploadDropbox);
        thread.start();
    }

    protected void getUserAccount() {

        new UserAccountTask(DropboxClient.getClient(mAccessToken), new UserAccountTask.TaskDelegate() {
            @Override
            public void onAccountReceived(FullAccount account) {
                showAccount(account);
            }

            @Override
            public void onError(Exception error) {
                Log.d("User", "gagal mendapatkan akun detail Dropbox");
                Toast.makeText(mContext, "Gagal login ke Dropbox", Toast.LENGTH_SHORT).show();
//                mButtonCamera.setEnabled(false);
            }
        }).execute();
    }

    private void showAccount(FullAccount account) {
        String name = account.getName().getDisplayName();
        String email = account.getEmail();
        Toast.makeText(getApplicationContext(), "Login akun Dropbox: " + name + ", " + email, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        int uiOptions =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }
}
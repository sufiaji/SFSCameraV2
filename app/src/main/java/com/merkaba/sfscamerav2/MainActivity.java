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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.io.File;

import static android.os.Environment.DIRECTORY_PICTURES;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MainActivity extends AppCompatActivity {

//    private String ACCESS_TOKEN =
//            "aQFvPEG6hqcAAAAAAAAAATREys7x89cfhiXElv8w2kII9cBGQet9FaWuyfp47w5c";
    private CameraView mCamera;
    private int mUploadCounter;
    private int mLocalSaveCounter;
    private int mThreadFinish;
    private Button mButtonCamera;
    private Button mButtonUpload;
    private TextView mTextCounter;
    private int mMaxPictures = 5;
    private Context mContext;
    private String TAG = "JANCUK";
    private String mAccessToken;
    private int mTimerTime = 5;
    private boolean mFlash = true;
    private boolean mLocalSave = true;
    private boolean mReal = false;
    private String mSendTo = "dropbox";
    private String mServerIP = "192.168.100.149";
    private String mPort = "1883";
    private TextView mTextMode;
    private TextView mTextFlash;
    private TextView mTextLocal;
    private TextView mTextDelay;
    private TextView mTextNumImage;
    private TextView mTextSendTo;
    private FloatingActionButton mFab1;
    private FloatingActionButton mFab2;
    private FloatingActionButton mFab6;
    private TextView mTextMqttState;
    private int mDropboxTimer = 60;
    private String mClientID = "AndroidClient";
    private String mTopicSub = "topic/ayam/command";
    private String mTopicPub = "topic/ayam/picture";
    private MqttAndroidClient mMqttAndroidClient;
    private MqttConnectOptions mqttConnectOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mButtonCamera = findViewById(R.id.buttonCamera);
        mButtonUpload = findViewById(R.id.buttonUpload);
        mButtonUpload.setVisibility(View.GONE);
        mTextCounter = findViewById(R.id.counter);
        mTextMode = findViewById(R.id.txtMode);
        mTextFlash = findViewById(R.id.txtFlash);
        mTextLocal = findViewById(R.id.txtLocal);
        mTextDelay = findViewById(R.id.txtDelay);
        mTextNumImage = findViewById(R.id.txtNumImage);
        mTextSendTo = findViewById(R.id.txtDropbox);
        mFab1 = findViewById(R.id.fab1);
        mFab2 = findViewById(R.id.fab2);
        mFab6 = findViewById(R.id.fab6);
        mTextMqttState = findViewById(R.id.mqttState);
        mAccessToken = retrieveAccessToken();
        getTimerTime();
        getMaxPictures();
        getUserAccount();
        getFlash();
        getLocalSavingFlag();
        getMode();
        getServer();
        setSendTo();
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
                if(mReal) {
                    SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
                    mSendTo = prefs.getString("sendto", mSendTo);
                    if(mSendTo.equals("mqtt"))
                        MQTTpublish(result.getData());
                    else if (mSendTo.equals("dropbox"))
                        uploadDbx(result.getData(), mUploadCounter);
                } else
                    uploadDbx(result.getData(), mUploadCounter);
                if(mLocalSave) {
                    File pathname = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
//                    String imageName = "ayam_" + System.currentTimeMillis() + ".jpg";
                    String imageName = Utils.generateFilename();
                    File destFile = new File(pathname, imageName);
                    result.toFile(destFile, new FileCallback() {
                        @Override
                        public void onFileReady(@Nullable File file) {
                            if(mReal)
                                Toast.makeText(mContext, imageName + " saved", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getApplicationContext(), "Image of " + mLocalSaveCounter + " saved.", Toast.LENGTH_SHORT).show();
                            mLocalSaveCounter += 1;
                        }
                    });
                }
                if(!mReal) {
                    if (mUploadCounter < mMaxPictures) {
                        mUploadCounter += 1;
                        takePictureAfterDelay(mTimerTime);
                    } else {
                        mButtonCamera.setEnabled(true);
                    }
                }
                if(mReal && mSendTo.equals("dropbox")) {
                    takePictureAfterDelay(mDropboxTimer);
                }
            }
        });
    }

    private void setSendTo() {
        // by default send to Dropbox
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mSendTo = "dropbox";
        prefs.edit().putString("sendto", mSendTo);
        mTextSendTo.setText("Send to " + mSendTo + "(every " + Integer.toString(mDropboxTimer) + "s)");
        if(mReal) {
            mTextSendTo.setVisibility(View.VISIBLE);
            mFab6.setVisibility(View.VISIBLE);
            mButtonUpload.setVisibility(View.VISIBLE);
        } else {
            mTextSendTo.setVisibility(View.GONE);
            mFab6.setVisibility(View.GONE);
            mButtonUpload.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mMqttAndroidClient.isConnected()) {
            mMqttAndroidClient.disconnect();
            mMqttAndroidClient = null;
        }
    }

    public void onSendToButton(View view) {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mSendTo = prefs.getString("sendto", mSendTo);
        if(mSendTo.equals("mqtt")){
            mButtonUpload.setVisibility(View.VISIBLE);
            mSendTo = "dropbox";
            MQTTDisconnect();
            new LovelyTextInputDialog(this)
                .setTitle("Send to Dropbox")
                .setMessage("Delay between upload (currently " + Integer.toString(mDropboxTimer) + "s)" )
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(!text.isEmpty()) {
                            mDropboxTimer = Integer.parseInt(text);
                            Toast.makeText(MainActivity.this, "Delay between upload now is " + text + "s", Toast.LENGTH_SHORT).show();
                            mTextSendTo.setText("Send to dropbox (every " + Integer.toString(mDropboxTimer) + "s)");
                        } else {
                            mTextSendTo.setText("Send to dropbox (every " + Integer.toString(mDropboxTimer) + "s)");
                        }
                    }
                })
                .show();
        } else {
            mSendTo = "mqtt";
            mTextSendTo.setText("Send to local");
            mButtonUpload.setVisibility(View.GONE);
            new LovelyTextInputDialog(this)
                .setTitle("Mode")
                .setMessage("MQTT Broker IP (currently " + prefs.getString("ip", mServerIP) + ")" )
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(text.isEmpty())
                            mServerIP = prefs.getString("ip", mServerIP);
                        else
                            mServerIP = text;
                        prefs.edit().putString("ip", mServerIP).apply();
                        mTextMode.setText("Real");
                        mTextNumImage.setVisibility(View.GONE);
                        mTextDelay.setVisibility(View.GONE);
                        mFab1.setVisibility(View.GONE);
                        mFab2.setVisibility(View.GONE);
                        mFab6.setVisibility(View.VISIBLE);
                        mButtonCamera.setVisibility(View.GONE);
                        mButtonUpload.setVisibility(View.GONE);
                        mTextCounter.setVisibility(View.GONE);
                        mTextMqttState.setVisibility(View.VISIBLE);
                        mTextSendTo.setVisibility(View.VISIBLE);
                        MQTTConnectAndSubscribe();
                    }
                })
                .show();

        }
        prefs.edit().putString("sendto",mSendTo);
    }

    private String retrieveAccessToken() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "Token does not exist.");
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

    public void onUploadButton(View view) {
        mCamera.takePicture();
    }

    private void getMode() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mReal = prefs.getBoolean("mode", mReal);
        if(mReal) {
            mTextMode.setText("Real");
            mTextMqttState.setVisibility(View.VISIBLE);
        } else {
            mTextMode.setText("Demo");
            mTextMqttState.setVisibility(View.GONE);
        }

    }

    private void getServer() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mServerIP = prefs.getString("ip", mServerIP);
    }

    private void getFlash() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mFlash = prefs.getBoolean("flash", mFlash);
        if(mFlash)
            mTextFlash.setText("Flash ON");
        else
            mTextFlash.setText("Flash OFF");
    }

    private void getMaxPictures() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mMaxPictures = prefs.getInt("maxpictures", mMaxPictures);
        mTextNumImage.setText("Num images " + Integer.toString(mMaxPictures));
    }

    private void getLocalSavingFlag() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mLocalSave = prefs.getBoolean("islocal", mLocalSave);
        if(mLocalSave)
            mTextLocal.setText("Local storage ON");
        else
            mTextLocal.setText("Local storage OFF");
    }

    public void onPicCounterButton(View view) {
        String sCurrentMax = String.valueOf(mMaxPictures);
        new LovelyTextInputDialog(this)
                .setTitle("Max Images")
                .setMessage("Max images taken (currently " + sCurrentMax + " images)")
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(text.isEmpty()) return;
                        mMaxPictures = Integer.parseInt(text);
                        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
                        prefs.edit().putInt("maxpictures", mMaxPictures).apply();
                        Toast.makeText(MainActivity.this, "Max images taken is now " + text + " images", Toast.LENGTH_SHORT).show();
                        mTextNumImage.setText("Num Images " + Integer.toString(mMaxPictures));
                    }
                })
                .show();
    }

    private void getTimerTime() {
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        mTimerTime = prefs.getInt("timertime", mTimerTime);
        mTextDelay.setText("Delay " + Integer.toString(mTimerTime) + "s");
    }

    public void onTimerButton(View view) {
        String sCurrentTime = String.valueOf(mTimerTime);
        new LovelyTextInputDialog(this)
                .setTitle("Timer")
                .setMessage("Delay between capture (currently " + sCurrentTime + "s)" )
                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        if(text.isEmpty()) return;
                        mTimerTime = Integer.parseInt(text);
                        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
                        prefs.edit().putInt("timertime", mTimerTime).apply();
                        Toast.makeText(MainActivity.this, "Delay between capture now is " + text + " s", Toast.LENGTH_SHORT).show();
                        mTextDelay.setText("Delay " + Integer.toString(mTimerTime) + "s");
                    }
                })
                .show();

    }

    public void onFlashButton(View view) {
        mFlash = !mFlash;
        if(mFlash) {
            mCamera.setFlash(Flash.ON);
            Toast.makeText(getApplicationContext(), "Flash ON", Toast.LENGTH_SHORT).show();
            mTextFlash.setText("Flash ON");
        } else {
            mCamera.setFlash(Flash.OFF);
            Toast.makeText(getApplicationContext(), "Flash OFF", Toast.LENGTH_SHORT).show();
            mTextFlash.setText("Flash OFF");
        }
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("flash", mFlash).apply();
    }

    public void onLocalButton(View view) {
        mLocalSave = !mLocalSave;
        if(mLocalSave) {
            Toast.makeText(getApplicationContext(), "Local Storage is now ON", Toast.LENGTH_SHORT).show();
            mTextLocal.setText("Local storage ON");
        } else {
            Toast.makeText(getApplicationContext(), "Local Storage is now OFF", Toast.LENGTH_SHORT).show();
            mTextLocal.setText("Local storage OFF");
        }
        SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("islocal", mLocalSave).apply();
    }

    public void onModeButton(View view) {
        if(mReal) {
            // DEMO means data acquisition using blackbox
            Toast.makeText(MainActivity.this, "DEMO MODE is now active", Toast.LENGTH_SHORT).show();
            mTextMode.setText("Demo");
            mTextNumImage.setVisibility(View.VISIBLE);
            mTextDelay.setVisibility(View.VISIBLE);
            mFab1.setVisibility(View.VISIBLE);
            mFab2.setVisibility(View.VISIBLE);
            mFab6.setVisibility(View.GONE);
            mButtonCamera.setVisibility(View.VISIBLE);
            mTextCounter.setVisibility(View.VISIBLE);
            mTextMqttState.setVisibility(View.GONE);
            mTextSendTo.setVisibility(View.GONE);
            mButtonUpload.setVisibility(View.GONE);
            mButtonCamera.setVisibility(View.VISIBLE);
        } else {
            // REAL means real application
            // reset to default: send to Dropbox
            Toast.makeText(MainActivity.this, "REAL MODE is now active", Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
            mSendTo = "dropbox";
            prefs.edit().putString("sendto", mSendTo);
            mButtonCamera.setVisibility(View.GONE);
            mButtonUpload.setVisibility(View.VISIBLE);
            mTextSendTo.setVisibility(View.VISIBLE);
            mFab6.setVisibility(View.VISIBLE);
            mTextSendTo.setText("Send to dropbox (every " + Integer.toString(mDropboxTimer) + "s)");
            mTextMode.setText("Real");
            mFab1.setVisibility(View.GONE);
            mTextNumImage.setVisibility(View.GONE);
            mFab2.setVisibility(View.GONE);
            mTextDelay.setVisibility(View.GONE);

//            SharedPreferences prefs = getSharedPreferences("com.merkaba.sfscamerav2", Context.MODE_PRIVATE);
//            new LovelyTextInputDialog(this)
//                .setTitle("Mode")
//                .setMessage("MQTT Broker IP (currently " + prefs.getString("ip", mServerIP) + ")" )
//                .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
//                    @Override
//                    public void onTextInputConfirmed(String text) {
//                        if(text.isEmpty())
//                            mServerIP = prefs.getString("ip", mServerIP);
//                        else
//                            mServerIP = text;
//                        prefs.edit().putString("ip", mServerIP).apply();
//                        Toast.makeText(MainActivity.this, "REAL MODE is now active", Toast.LENGTH_SHORT).show();
//                        mTextMode.setText("Real");
//                        mTextNumImage.setVisibility(View.GONE);
//                        mTextDelay.setVisibility(View.GONE);
//                        mFab1.setVisibility(View.GONE);
//                        mFab2.setVisibility(View.GONE);
//                        mFab6.setVisibility(View.VISIBLE);
//                        mButtonCamera.setVisibility(View.GONE);
//                        mButtonUpload.setVisibility(View.GONE);
//                        mTextCounter.setVisibility(View.GONE);
//                        mTextMqttState.setVisibility(View.VISIBLE);
//                        mTextSendTo.setVisibility(View.VISIBLE);
//                    }
//                })
//                .show();
        }
        mReal = !mReal;
    }

    private void MQTTDisconnect() {
        mMqttAndroidClient.disconnect();
        mMqttAndroidClient = null;
        mTextMqttState.setText("MQTT state disconnected.");
        Toast.makeText(mContext, "MQTT disconnected.", Toast.LENGTH_SHORT).show();
    }

    private void MQTTConnectAndSubscribe() {
        String mqttServer = "tcp://" + mServerIP + ":" + mPort;
        mMqttAndroidClient = new MqttAndroidClient(getApplicationContext(), mqttServer, mClientID, Ack.AUTO_ACK);
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setPassword(("Jakarta@01").toCharArray());
        mqttConnectOptions.setUserName("admin");

        mMqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect) {
                    mTextMqttState.setText("Reconnected to :" + mqttServer);
                    // auto subs
                    MQTTsubscribe();
                } else {
                    mTextMqttState.setText("Connected to :" + mqttServer);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                mTextMqttState.setText("Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // accept string only for the moment
                String command = new String(message.getPayload());
                mTextMqttState.setText("Command: " + command);
                handleCommand(command);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Toast.makeText(MainActivity.this, "Delivery Completed", Toast.LENGTH_SHORT).show();
            }
        });

        mMqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(MainActivity.this, "Connected to: " + mqttServer, Toast.LENGTH_SHORT).show();
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                mMqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                MQTTsubscribe();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCommand(String cmd) {
        if(cmd.equals("send_me_image")) {
            mCamera.takePicture();
        }
    }

    private void MQTTpublish(byte[] data) {
        if(!mMqttAndroidClient.isConnected()) {
            Toast.makeText(this, "MQTT not connected.", Toast.LENGTH_SHORT).show();
            return;
        }
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(data);
        mMqttAndroidClient.publish(mTopicPub, mqttMessage);
    }

    private void MQTTsubscribe() {
        mMqttAndroidClient.subscribe(mTopicSub, 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(MainActivity.this, "Subscribed to " + mTopicSub, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(MainActivity.this, "Failed to subscribed to " + mTopicSub, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadDbx(byte[] data, int counter) {
        new UploadFileTask(mContext, DropboxClient.getClient(mAccessToken), data, counter, new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result, int counter) {
                if(!mReal) {
                    mThreadFinish += 1;
                    Toast.makeText(mContext, "Image " + Integer.toString(counter) + " sent to Dropbox.", Toast.LENGTH_SHORT).show();
                    mTextCounter.setText(Integer.toString(mThreadFinish) + "/" + mMaxPictures);
                    if (mThreadFinish == mMaxPictures) ding();
                } else {
                    Toast.makeText(mContext, "Image " + Integer.toString(counter) + " uploaded successfully.", Toast.LENGTH_SHORT).show();
                    ding();
                }
            }

            @Override
            public void onError(Exception e, int counter) {
                if(!mReal) {
                    mThreadFinish += 1;
                    Toast.makeText(mContext, "Image " + Integer.toString(counter) + " failed to send to Dropbox.", Toast.LENGTH_SHORT).show();
                    mTextCounter.setText(Integer.toString(mThreadFinish) + "/" + mMaxPictures);
                    if (mThreadFinish == mMaxPictures) ding();
                } else {
                    Toast.makeText(mContext, "Image " + Integer.toString(counter) + " failed to upload.", Toast.LENGTH_SHORT).show();
                }
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
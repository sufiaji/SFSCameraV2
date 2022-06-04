package com.merkaba.sfscamerav2;
import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Async task to upload a file to a directory
 */
class UploadFileTask extends AsyncTask<String, Void, FileMetadata> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private byte[] mData;
    private int mCounter;

    public interface Callback {
        void onUploadComplete(FileMetadata result, int counter);
        void onError(Exception e, int counter);
    }

    public UploadFileTask(Context context, DbxClientV2 dbxClient, byte[] data, int counter, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
        mData = data;
        mCounter = counter;
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException, mCounter);
        } else if (result == null) {
            mCallback.onError(null, mCounter);
        } else {
            mCallback.onUploadComplete(result, mCounter);
        }
    }

    private String generateName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        String replaced = strDate.replace(":", "-");
        replaced = replaced.replace(" ", "_");
        replaced = "ayam_" + replaced + ".jpg";
        return replaced;
    }

    @Override
    protected FileMetadata doInBackground(String... params) {
        try(InputStream inputStream = new ByteArrayInputStream(mData)) {
            // Upload to Dropbox

            String filename = Utils.generateFilename();
            return mDbxClient.files().uploadBuilder("/Ayam_Mgu_01/" + filename) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream)
            ;
        } catch (DbxException | IOException e) {
            mException = e;
        }

        return null;
    }
}

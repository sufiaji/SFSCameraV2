package com.merkaba.sfscamerav2;

import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadDropbox implements Runnable {
    private byte[] mData;
    private DbxClientV2 mDbxClientV2;

    public UploadDropbox(byte[] data, DbxClientV2 dbxClientV2) {
        mData = data;
        mDbxClientV2 = dbxClientV2;
    }

    private String generateName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        String replaced = strDate.replace(":", "-");
        replaced = replaced.replace(" ", "_");
        replaced = replaced + ".jpg";
        return replaced;
    }

    @Override
    public void run() {
        try {
            // Upload to Dropbox
            InputStream inputStream = new ByteArrayInputStream(mData);
            String filename = generateName();
            mDbxClientV2.files().uploadBuilder("/Ayam_Mgu_01/" + filename) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream)
            ;
            Log.d("Upload Status", "Success");
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

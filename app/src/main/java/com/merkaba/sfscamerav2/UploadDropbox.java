package com.merkaba.sfscamerav2;

import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadDropbox implements Runnable {
    private byte[] mData;
    private DbxClientV2 mDbxClientV2;

    public UploadDropbox(byte[] data, DbxClientV2 dbxClientV2) {
        mData = data;
        mDbxClientV2 = dbxClientV2;
    }

    @Override
    public void run() {
        try {
            // Upload to Dropbox
            InputStream inputStream = new ByteArrayInputStream(mData);
            String filename = Utils.generateFilename();
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

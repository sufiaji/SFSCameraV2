package com.merkaba.sfscamerav2;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String generateFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        String replaced = strDate.replace(":", "-");
        replaced = replaced.replace(" ", "_");
        replaced = "ayam_" + replaced + ".jpg";
        return replaced;
    }
}

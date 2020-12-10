package com.colin.mosaicdemo.util;

import android.content.res.Resources;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

/**
 * create by colin
 * 2020/12/10
 */
public class SizeUtils {

    public static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    public static float computeFitScale(@NonNull Bitmap srcBitmap, int viewWidth, int viewHeight) {
        float fitscale;

        float drawMargin = 0f;

        float imgW = srcBitmap.getWidth();
        float imgH = srcBitmap.getHeight();

        float scaleWidth = (viewWidth - drawMargin) / imgW;

        float scaleHeight = (viewHeight - drawMargin) / imgH;

        fitscale = Math.min(scaleWidth, scaleHeight);

        return fitscale;
    }
}

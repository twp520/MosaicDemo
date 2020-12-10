package com.colin.mosaicdemo.art;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.colin.mosaicdemo.MyApp;
import com.colin.mosaicdemo.mosaic.MosaicPainter;
import com.colin.mosaicdemo.mosaic.MosaicPath;

import java.util.List;

/**
 * create by colin
 * 2020/11/16
 * <p>
 * 不使用 BitmapShader 来画纹理，单独创建一张bitmap作为一个图层来画纹理。
 */
public class PersonPainter extends MosaicPainter {

    //人像识别出的路径
    private Path personPath;
    private float personPathScale;
    //抠人像用的
    private Canvas personCanvas;
    private Bitmap personBitmap;
    //黑白化的原图
    private Bitmap blackSrcBitmap;
    //羽化滤镜
    private BlurMaskFilter blurMaskFilter;
    //抠人像的画笔
    private Paint paintSrcIn;

    private Canvas textureCanvas;
    private Bitmap textureBitmap;

    public PersonPainter(Context context) {
        super(context);
    }

    public PersonPainter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void setPathPaint(Paint pathPaint) {
        super.setPathPaint(pathPaint);
        //设置blur
        paintSrcIn = new Paint();
        paintSrcIn.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        paintSrcIn.setAntiAlias(true);
        paintSrcIn.setFilterBitmap(true);
        paintSrcIn.setDither(true);
    }

    protected void preDrawAll(Canvas pathCanvas, Paint pathPaint, boolean needBlur) {
        //画原始的路径
        staticPreDrawAll(pathCanvas, pathPaint, needBlur, blurMaskFilter, personPath, personPathScale);
    }


    @Override
    protected void drawPathToCanvas() {
        textureCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        textureCanvas.save();
        textureCanvas.drawColor(Color.parseColor("#80D62626"));
        textureCanvas.drawBitmap(mPathBitmap, 0, 0, paintSrcIn);
        textureCanvas.restore();
        mResultCanvas.drawBitmap(textureBitmap, 0, 0, mPaintSrc);
    }

    public void setPersonAndBitmaps(Bitmap srcBitmap, Path personPath, float personPathScale) {
        personBitmap = Glide.get(getContext()).getBitmapPool()
                .get(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        textureBitmap = Glide.get(getContext()).getBitmapPool()
                .get(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        personCanvas = new Canvas(personBitmap);
        textureCanvas = new Canvas(textureBitmap);
        this.personPath = personPath;
        this.personPathScale = personPathScale;
        blurMaskFilter = new BlurMaskFilter(getBlurRadius(srcBitmap), BlurMaskFilter.Blur.NORMAL);
        float fitScale = computeFitScale(srcBitmap);
        getViewCamera().reset();
        getViewCamera().setViewScale(fitScale, fitScale, fitScale * 2f);
        setBitmaps(srcBitmap, srcBitmap, false);
    }

    public void resetScale() {
        if (personBitmap != null) {
            float fitScale = computeFitScale(personBitmap);
            getViewCamera().reset();
            getViewCamera().setViewScale(fitScale, fitScale, fitScale * 2f);
        }
    }

    private float computeFitScale(Bitmap sourceBitmap) {
        float fitScale;
        float drawMargin = mBottomMargin;
        float imgW = sourceBitmap.getWidth();
        float imgH = sourceBitmap.getHeight();
        float scaleWidth = getWidth() / imgW;
        float scaleHeight = (getHeight() - drawMargin) / imgH;
        fitScale = Math.min(scaleWidth, scaleHeight);
        return fitScale;
    }

    public void setMskModel() {
        currentModel = MosaicPath.TYPE_SMUDGE;
        selectMskModel = MosaicPath.TYPE_SMUDGE;
    }

    public void setBlackSrcBitmap(Bitmap blackSrcBitmap) {
        this.blackSrcBitmap = blackSrcBitmap;
    }

    public static void drawPersonBitmap(Canvas personCanvas,
                                        Bitmap srcBitmap, Bitmap pathBitmap,
                                        Paint paintSrcIn, Paint paintSrc) {
        //直接srcIn抠出来
        personCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        personCanvas.save();
        personCanvas.drawBitmap(srcBitmap, 0, 0, paintSrc);
        personCanvas.drawBitmap(pathBitmap, 0, 0, paintSrcIn);
        personCanvas.restore();
    }


    public Bitmap createPersonBitmap(boolean needGray, boolean needBlur) {

        staticCreatePersonBitmap(needGray, needBlur, blackSrcBitmap, mSrcBitmap,
                mPaintPath, mPaintClean, paintSrcIn, mPaintSrc, personCanvas,
                personPath, personPathScale, getPathList());
        return personBitmap;
    }

    public void cleanBitmap() {
        super.cleanBitmap();
        if (personBitmap != null) {
            personBitmap.recycle();
        }
        if (personCanvas != null) {
            personCanvas.setBitmap(null);
        }
        if (textureBitmap != null) {
            textureBitmap.recycle();
        }
        if (textureCanvas != null) {
            textureCanvas.setBitmap(null);
        }
    }

    public MosaicPath getPersonPath() {
        MosaicPath path = new MosaicPath();
        path.path = new Path(personPath);
        path.size = personPathScale;
        path.type = MosaicPath.TYPE_SMUDGE;
        return path;
    }

    public static float getBlurRadius(Bitmap bitmap) {
        return bitmap == null ? 0 : Math.max(bitmap.getWidth(), bitmap.getHeight()) * 0.0085f;
    }

    @Override
    protected boolean isEmptyEraserModel() {
        return personPath == null && super.isEmptyEraserModel();
    }

    public static void staticCreatePersonBitmap(boolean needGray, boolean needBlur,
                                                Bitmap blackSrcBitmap, Bitmap srcBitmap,
                                                Paint paintPath, Paint paintClean, Paint paintSrcIn, Paint paintSrc,
                                                Canvas personCanvas, Path personPath, float personPathScale,
                                                List<MosaicPath> pathList) {
        Bitmap sourceBitmap;
        if (needGray && blackSrcBitmap != null) {
            sourceBitmap = blackSrcBitmap;
        } else {
            sourceBitmap = srcBitmap;
        }
        BlurMaskFilter blurMaskFilter = new BlurMaskFilter(getBlurRadius(sourceBitmap), BlurMaskFilter.Blur.NORMAL);
        Bitmap pathBitmap = Glide.get(MyApp.appContext).getBitmapPool()
                .get(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //画出路径
        Canvas pathCanvas = new Canvas(pathBitmap);
        staticPreDrawAll(pathCanvas, paintPath, needBlur, blurMaskFilter, personPath, personPathScale);
        if (needBlur) {
            paintPath.setMaskFilter(blurMaskFilter);
            paintClean.setMaskFilter(blurMaskFilter);
        }
        for (MosaicPath mosaicPath : pathList) {
            drawSinglePath(mosaicPath, pathCanvas, paintClean, paintPath);
        }
        paintPath.setMaskFilter(null);
        paintClean.setMaskFilter(null);
        drawPersonBitmap(personCanvas, sourceBitmap, pathBitmap, paintSrcIn, paintSrc);
        pathBitmap.recycle();
        pathCanvas.setBitmap(null);
    }

    public static void staticPreDrawAll(Canvas pathCanvas, Paint pathPaint, boolean needBlur,
                                        BlurMaskFilter blurMaskFilter, Path personPath, float personPathScale) {
        if (personPath != null) {
            //演示方便，直接注释
            /*if (needBlur)
                pathPaint.setMaskFilter(blurMaskFilter);
            pathPaint.setStrokeCap(Paint.Cap.BUTT);
            pathCanvas.save();
            pathCanvas.translate(personPathScale / 2f, personPathScale / 2f);
            pathPaint.setStrokeWidth(personPathScale);
            pathCanvas.drawPath(personPath, pathPaint);
            pathPaint.setMaskFilter(null);
            pathPaint.setStrokeCap(Paint.Cap.ROUND);
            pathCanvas.restore();
        }*/
            pathPaint.setStyle(Paint.Style.FILL);
            pathCanvas.drawPath(personPath, pathPaint);
            pathPaint.setStyle(Paint.Style.STROKE);
        }
    }
}

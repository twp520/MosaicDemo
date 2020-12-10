package com.colin.mosaicdemo.art;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.colin.mosaicdemo.util.Logger;
import com.colin.mosaicdemo.util.SizeUtils;

/**
 * create by colin
 * 2020/11/17
 * <p>
 * 双重曝光view
 */
public class DoubleExposureView extends View {

    //人像图
    private Bitmap personBitmap;
    //素材图
    private Bitmap textureBitmap;

    //普通画笔
    private Paint paintSrc;
    //混合画笔
    private Paint paintBlend;
    //素材边框的画笔
    private Paint paintBord;

    //绘制距离底部的距离
    private int mBottomMargin;
    //手指信息
    public PointerInfo pointerInfo;

    //人像图相对于画布的缩放值
//    private float targetFitScale;

    //素材边框的路径,素材边框变换后的路径
    private Path textureRect, textureDstRect;
    private RectF texturePositionRect, texturePositionDstRect;

    //人像图需要适应画图，所以使用矩阵进行变换。
    private Matrix personMatrix;
    //人像图的矩形，人像图变换后的矩形。
    private RectF personRect, personDstRect;

    private CallBack callBack;

    private Logger logger;


    public DoubleExposureView(Context context) {
        super(context);
        init();
    }

    public DoubleExposureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DoubleExposureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintSrc = new Paint();
        paintSrc.setFilterBitmap(true);
        paintSrc.setAntiAlias(true);
        paintSrc.setDither(true);
        paintSrc.setColor(Color.WHITE);

        paintBlend = new Paint();
        paintBlend.setFilterBitmap(true);
        paintBlend.setAntiAlias(true);
        paintBlend.setDither(true);
        paintBlend.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        paintBord = new Paint();
        paintBord.setStrokeWidth(SizeUtils.dp2px(1f));
        paintBord.setStyle(Paint.Style.STROKE);
        paintBord.setColor(Color.parseColor("#ac9600"));
        paintBord.setAntiAlias(true);

        pointerInfo = new PointerInfo(new Matrix());
        pointerInfo.setMaxZoom(3f);
        pointerInfo.setMinZoom(0.1f);

        textureRect = new Path();
        textureDstRect = new Path();

        texturePositionRect = new RectF();
        texturePositionDstRect = new RectF();

        personMatrix = new Matrix();
        personRect = new RectF();
        personDstRect = new RectF();

        logger = new Logger();
    }

    public void setBitmaps(Bitmap personBitmap, Bitmap textureBitmap) {
        setPersonBitmap(personBitmap, false);
        setTextureBitmap(textureBitmap, false, true);
        invalidate();
    }

    public void setPersonBitmap(Bitmap personBitmap, boolean invalidate) {
        this.personBitmap = personBitmap;
        float scaleW = getCanvasWidth() * 1f / personBitmap.getWidth();
        float scaleH = getCanvasHeight() * 1f / personBitmap.getHeight();
        float targetFitScale = Math.min(scaleW, scaleH);
        personMatrix.reset();
        personMatrix.postScale(targetFitScale, targetFitScale,
                personBitmap.getWidth() / 2f, personBitmap.getHeight() / 2f);
        personMatrix.postTranslate((getCanvasWidth() - personBitmap.getWidth()) / 2f,
                (getCanvasHeight() - personBitmap.getHeight()) / 2f);
        personRect.set(0, 0, personBitmap.getWidth(), personBitmap.getHeight());
        personMatrix.mapRect(personDstRect, personRect);
        pointerInfo.setPersonDstRect(personDstRect);
        logger.debug("personBitmap size = [" + personBitmap.getWidth() + "," + personBitmap.getHeight() + "]");
        logger.debug("personBitmap scaleW = " + scaleW + ", scaleH = " + scaleH + ", targetScale = " + targetFitScale);
        logger.debug("personRect = " + personRect.toShortString() + ", personDstRect = " + personDstRect.toShortString());
        if (invalidate) {
            invalidate();
        }
    }

    public void setTextureBitmap(Bitmap textureBitmap, boolean invalidate, boolean reset) {
        this.textureBitmap = textureBitmap;
        if (reset) {
            texturePositionRect.set(0, 0, textureBitmap.getWidth(), textureBitmap.getHeight());
            textureRect.reset();
            textureDstRect.reset();
            textureRect.addRect(new RectF(0, 0, textureBitmap.getWidth(), textureBitmap.getHeight()), Path.Direction.CW);
            float targetSize = Math.max(personDstRect.width(), personDstRect.height()) * 1.2f;
            float scale = targetSize / textureBitmap.getWidth();
            pointerInfo.getTextureMatrix().reset();
            pointerInfo.getTextureMatrix().postScale(scale, scale,
                    textureBitmap.getWidth() / 2f, textureBitmap.getHeight() / 2f);
            float dx = (getCanvasWidth() - textureBitmap.getWidth()) / 2f;
            float dy = (getCanvasHeight() - textureBitmap.getHeight()) / 2f;
            pointerInfo.getTextureMatrix().postTranslate(dx, dy);
            textureRect.transform(pointerInfo.getTextureMatrix(), textureDstRect);
            pointerInfo.getTextureMatrix().mapRect(texturePositionDstRect, texturePositionRect);
            pointerInfo.setTexturePositionRect(texturePositionDstRect);
            logger.info("textureBitmap size = [" + textureBitmap.getWidth() + "," + textureBitmap.getHeight() + "]");
            logger.info(" targetSize = " + targetSize + ", scale = " + scale + ", dx = " + dx + ",dy = " + dy);
        }
        if (invalidate)
            invalidate();
    }

    public void setBottomMargin(int bottomMargin) {
        this.mBottomMargin = bottomMargin;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (personBitmap == null || textureBitmap == null) {
            canvas.drawColor(Color.WHITE);
            return;
        }

        int personCount = canvas.save();
        paintSrc.setStyle(Paint.Style.FILL);
        canvas.drawRect(personDstRect, paintSrc);
        canvas.drawBitmap(personBitmap, personMatrix, paintSrc);
        canvas.restoreToCount(personCount);

        //绘制素材
        int textureCount = canvas.save();
        canvas.clipRect(personDstRect);
        canvas.drawBitmap(textureBitmap, pointerInfo.getTextureMatrix(), paintBlend);
        canvas.restoreToCount(textureCount);

        //绘制边框
        int bordSave = canvas.save();
        textureRect.transform(pointerInfo.getTextureMatrix(), textureDstRect);
        pointerInfo.getTextureMatrix().mapRect(texturePositionDstRect, texturePositionRect);
        canvas.drawPath(textureDstRect, paintBord);
        canvas.restoreToCount(bordSave);


    }

    public int getCanvasWidth() {
        return getWidth();
    }

    public int getCanvasHeight() {
        return getHeight() - mBottomMargin;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean consume = false;
        try {
            int actionIndex = event.getActionIndex();
            int pointerId = event.getPointerId(actionIndex);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pointerInfo.setActionPointerId(pointerId);
                    pointerInfo.setMoveType(PointerInfo.TYPE_TRANSLATION, event, true);
                    consume = true;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (pointerInfo.getScalePointerId() == MotionEvent.INVALID_POINTER_ID) {
                        pointerInfo.setScalePointerId(pointerId);
                        pointerInfo.setMoveType(PointerInfo.TYPE_SCALE, event, true);
                        consume = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pointerInfo.getMoveType() == PointerInfo.TYPE_TRANSLATION
                            && pointerInfo.getActionPointerId() != MotionEvent.INVALID_POINTER_ID) {
                        //移动
                        pointerInfo.calculationTranslation(event);
                        invalidate();
                        consume = true;
                    } else if (pointerInfo.getMoveType() == PointerInfo.TYPE_SCALE
                            && pointerInfo.getScalePointerId() != MotionEvent.INVALID_POINTER_ID) {
                        //缩放或者旋转
                        pointerInfo.calculationScaleAndRotation(event, getCanvasWidth() / 2f, getCanvasHeight() / 2f);
                        invalidate();
                        consume = true;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if (pointerId == pointerInfo.getActionPointerId()) {
                        pointerInfo.setActionPointerId(MotionEvent.INVALID_POINTER_ID);
                    } else if (pointerId == pointerInfo.getScalePointerId()) {
                        pointerInfo.setScalePointerId(MotionEvent.INVALID_POINTER_ID);
                    }

                    if (pointerInfo.getActionPointerId() != MotionEvent.INVALID_POINTER_ID) {
                        if (pointerInfo.getScalePointerId() != MotionEvent.INVALID_POINTER_ID) {
                            pointerInfo.setMoveType(PointerInfo.TYPE_SCALE, event, false);
                        } else {
                            pointerInfo.setMoveType(PointerInfo.TYPE_TRANSLATION, event, true);
                        }
                    } else {
                        pointerInfo.setMoveType(PointerInfo.TYPE_NONE, event, false);
                    }
                    consume = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    pointerInfo.setActionPointerId(MotionEvent.INVALID_POINTER_ID);
                    pointerInfo.setScalePointerId(MotionEvent.INVALID_POINTER_ID);
                    pointerInfo.setMoveType(PointerInfo.TYPE_NONE, event, false);
                    consume = true;
                    break;
            }
            if (consume && callBack != null) {
                callBack.onChanged();
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return consume;
    }


    public Matrix getTextureMatrix() {
        return pointerInfo.getTextureMatrix();
    }

    public int getTextureBitmapWidth() {
        return textureBitmap.getWidth();
    }

    public int getTextureBitmapHeight() {
        return textureBitmap.getHeight();
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public interface CallBack {
        //暂时不需要对平移和缩放做特殊处理
        void onChanged();
    }

}

package com.colin.mosaicdemo;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;


/**
 * @author wxfred
 */
public class ViewCamera implements ScalePanGestureDetector.OnScalePanGestureListener {

    private static final String TAG = "View Camera";
    private float mbottomMargin;

    public static interface ViewCameraListener {
        public void onViewScaleChanged(float viewScale);
    }

    private View mView;
    private float[] mViewPos;
    private float mViewXLast, mViewYLast;
    private float mViewScale;
    private float mViewScaleLast;
    private float mViewScaleMin, mViewScaleDefault, mViewScaleMax;
    private PassiveAnimation mTranslateAnimation;
    private PassiveAnimation mScaleAnimation;

    private float mImageWidth, mImageHeight;

    private ScalePanGestureDetector mFotorScalePanGestureDetector;
    private float mScaleFocusXLast;
    private float mScaleFocusYLast;
    boolean mIsScaling;
    boolean mIsAnimating;

    boolean mIsBlock;

    private ViewCameraListener mCameraListener;

    /**
     * Constructor
     */
    public ViewCamera(View view) {
        mView = view;
        mViewPos = new float[2];
        mViewScale = 1.0f;
        mViewScaleDefault = 1.0f;
        mViewScaleMin = mViewScaleDefault;
        mViewScaleMax = mViewScaleDefault * 5.0f;

        mTranslateAnimation = new PassiveAnimation();
        mTranslateAnimation.setInterpolator(new DecelerateInterpolator());
        mScaleAnimation = new PassiveAnimation();
        mScaleAnimation.setInterpolator(new DecelerateInterpolator());

        mFotorScalePanGestureDetector = new ScalePanGestureDetector(this);

        mIsScaling = false;
        mIsAnimating = false;

        mIsBlock = true;
    }

    /**
     * @param canvas
     */
    public void onDrawInit(Canvas canvas) {
        onDrawInit(canvas, 0f);
    }

    /**
     * @param canvas
     */
    public void onDrawInit(Canvas canvas, float bottomMargin) {
        this.mbottomMargin = bottomMargin;
        handleAnimations();

        canvas.save();
        // 设置视口，将绘制原点置为屏幕中心
        canvas.translate(mView.getWidth() / 2.0f, (mView.getHeight() - mbottomMargin) / 2.0f);
        // 整体缩放
        canvas.scale(mViewScale, mViewScale);
        // 移动至视点位置
        canvas.translate(-mViewPos[0], -mViewPos[1] /*- bottomMargin/2f*/);
    }

    public void setViewCameraListener(ViewCameraListener listener) {
        mCameraListener = listener;
    }

    // Getter(s)
    public boolean isScaling() {
        return mIsScaling;
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    // Setter(s)
    public void setView(View view) {
        mView = view;
    }

    public void setImageSize(float width, float height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    public void setViewScale(float min, float def, float max) {
        mViewScaleMin = min;
        mViewScaleDefault = def;
        mViewScaleMax = max;

        mViewScale = def;

        if (mCameraListener != null) {
            mCameraListener.onViewScaleChanged(mViewScale);
        }
    }

    public float getViewScale() {
        return mViewScale;
    }

    /**
     * @param canvas
     */
    public void onDrawEnd(Canvas canvas) {
        canvas.restore();
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mFotorScalePanGestureDetector.onTouchEvent(event);
    }

    /**
     * @param block
     */
    public void setBlock(boolean block) {
        mIsBlock = block;
        if (mIsBlock) {
            mFotorScalePanGestureDetector.setToTwoPointerTouch();
        } else {
            mFotorScalePanGestureDetector.setToMultiPointerTouch();
        }
    }

    public final float[] mapToScenesFromView(float x, float y) {
        float[] pos = new float[2];

        pos[0] = mViewPos[0] + (x - mView.getWidth() / 2.0f) / mViewScale;
        pos[1] = mViewPos[1] + (y - (mView.getHeight() - mbottomMargin) / 2.0f) / mViewScale;

        return pos;
    }

    public final float[] mapToImageFromScenes(float x, float y) {
        float[] pos = new float[2];

        pos[0] = x + mImageWidth / 2.0f;
        pos[1] = y + mImageHeight / 2.0f;

        return pos;
    }

    public final float[] mapToImageFromView(float x, float y) {
        float[] pos = mapToScenesFromView(x, y);
        return mapToImageFromScenes(pos[0], pos[1]);
    }

    public final float[] mapToScenesFromImage(float x, float y) {
        float[] pos = new float[2];

        pos[0] = x - mImageWidth / 2.0f;
        pos[1] = y - mImageHeight / 2.0f;

        return pos;
    }

    public final float[] mapToViewFromScenes(float x, float y) {
        float[] pos = new float[2];

        pos[0] = (x - mViewPos[0]) * mViewScale + mView.getWidth() / 2.0f;
        pos[1] = (y - mViewPos[1]) * mViewScale + (mView.getHeight() - mbottomMargin) / 2.0f;

        return pos;
    }

    public final float[] mapToViewFromImage(float x, float y) {
        float[] pos = mapToScenesFromImage(x, y);
        return mapToViewFromScenes(pos[0], pos[1]);
    }

    public final RectF mapToScenesFromImage(RectF rect) {
        float[] leftTop = mapToScenesFromImage(rect.left, rect.top);
        float[] rightBottom = mapToScenesFromImage(rect.right, rect.bottom);
        return new RectF(leftTop[0], leftTop[1], rightBottom[0], rightBottom[1]);
    }

    public final RectF mapToViewFromScenes(RectF rect) {
        float[] leftTop = mapToViewFromScenes(rect.left, rect.top);
        float[] rightBottom = mapToViewFromScenes(rect.right, rect.bottom);
        return new RectF(leftTop[0], leftTop[1], rightBottom[0], rightBottom[1]);
    }

    // Scale pan listeners
    @Override
    public void onScalePanFocus(float focusX, float focusY) {
        Log.i(TAG, "scale pan focus");

        // 强制停止缩放平移动画
        mScaleAnimation.stop();
        mTranslateAnimation.stop();
        mIsAnimating = false;

        mViewScaleLast = mViewScale;
        mViewXLast = mViewPos[0];
        mViewYLast = mViewPos[1];
        mScaleFocusXLast = focusX;
        mScaleFocusYLast = focusY;

        mIsScaling = true;
    }

    @Override
    public void onScalePan(float scaleFactor, float panX, float panY) {

        // 屏幕中心缩放
        float scale = scaleFactor;
        float viewScale;
        viewScale = mViewScaleLast * scale;
        if (mIsBlock) {
            // 超过范围则减缓缩放响应强度
            float obstacle = 2.0f;
            if (viewScale < mViewScaleMin)
                viewScale = mViewScaleMin
                        * (float) Math.pow(obstacle, viewScale / mViewScaleMin
                        - 1.0f);
            if (viewScale > mViewScaleMax)
                viewScale = mViewScaleMax
                        / (float) Math.pow(obstacle, mViewScaleMax / viewScale
                        - 1.0f);
            scale = viewScale / mViewScaleLast;
        }
        mViewScale = viewScale;
        if (mCameraListener != null) {
            mCameraListener.onViewScaleChanged(mViewScale);
        }

        // 位移视口，使其变为两指中心缩放
        float viewX, viewY;
        viewX = mViewXLast - (mScaleFocusXLast - mView.getWidth() / 2.0f)
                * (1.0f - scale) / viewScale;
        viewY = mViewYLast - (mScaleFocusYLast - (mView.getHeight() - mbottomMargin) / 2.0f)
                * (1.0f - scale) / viewScale;

        // 两指中心平移
        viewX -= panX / viewScale;
        viewY -= panY / viewScale;
        mViewPos[0] = viewX;
        mViewPos[1] = viewY;
        // 越界限制
        if (mIsBlock) {
            float[] finalPos = restrictViewPos(viewX, viewY, viewScale);
            float vecX = viewX - finalPos[0];
            float vecY = viewY - finalPos[1];
            float a = 0.1f;
            mViewPos[0] = finalPos[0]
                    + (1.0f - (float) Math.pow(a,
                    Math.abs(vecX * viewScale / mView.getWidth())))
                    * positive(vecX) * mView.getWidth() / 4.0f / viewScale;
            mViewPos[1] = finalPos[1]
                    + (1.0f - (float) Math.pow(a,
                    Math.abs(vecY * viewScale / (mView.getHeight() - mbottomMargin))))
                    * positive(vecY) * (mView.getHeight() - mbottomMargin) / 4.0f / viewScale;
        }

        mView.postInvalidate();
    }

    @Override
    public void onScalePanEnd() {
        Log.i(TAG, "scale pan end");
        int duration = 200;
        float finalScale = mViewScale;
        float[] end = null;

        if (mScaleAnimation.isStopped() == false)
            finalScale = mScaleAnimation.getEndValue();

        // 约束最小缩放值
        if (mViewScale < mViewScaleMin) {
            finalScale = mViewScaleMin;
            end = new float[1];
            end[0] = mViewScaleMin;
            mScaleAnimation.setStartValue(mViewScale);
            mScaleAnimation.setEndValues(end);
            mScaleAnimation.setDuration(duration);
            mScaleAnimation.start();
        }
        // 约束最大缩放值
        if (mViewScale > mViewScaleMax) {
            finalScale = mViewScaleMax;
            end = new float[1];
            end[0] = mViewScaleMax;
            mScaleAnimation.setStartValue(mViewScale);
            mScaleAnimation.setEndValues(end);
            mScaleAnimation.setDuration(duration);
            mScaleAnimation.start();
        }

        // 约束视口坐标
        float[] finalPos = restrictViewPos(mViewPos[0], mViewPos[1], finalScale);
        if (finalPos[0] != mViewPos[0] || finalPos[1] != mViewPos[1]) {
            end = new float[2];
            end[0] = finalPos[0];
            end[1] = finalPos[1];
            mTranslateAnimation.setStartValues(mViewPos);
            mTranslateAnimation.setEndValues(end);
            mTranslateAnimation.setDuration(duration);
            mTranslateAnimation.start();
        }

        mIsScaling = false;
        if (mScaleAnimation.isStopped() == false
                || mTranslateAnimation.isStopped() == false) {
            mIsAnimating = true;
        }

        mView.postInvalidate();
    }

    protected final float[] restrictViewPos(float x, float y, float scale) {
        float[] finalPos = new float[2];

        // 约束横坐标
        float finalWidth = mImageWidth * scale;
        float finalX = x;
        if (finalWidth < mView.getWidth()) {
            // 横向居中
            finalX = 0.0f;
        } else {
            float halfSpan = mView.getWidth() / 2.0f / scale;
            float left = -mImageWidth / 2.0f;
            float right = mImageWidth / 2.0f;
            if (x - halfSpan < left) {
                finalX = left + halfSpan;
            }
            if (x + halfSpan > right) {
                finalX = right - halfSpan;
            }
        }

        // 约束纵坐标
        float finalHeight = mImageHeight * scale;
        float finalY = y;
        if (finalHeight < (mView.getHeight() - mbottomMargin)) {
            // 纵向居中
            finalY = 0.0f;
        } else {
            float halfSpan = (mView.getHeight() - mbottomMargin) / 2.0f / scale;
            float top = mImageHeight / 2.0f;
            float bottom = -mImageHeight / 2.0f;
            if (y + halfSpan > top) {
                finalY = top - halfSpan;
            }
            if (y - halfSpan < bottom) {
                finalY = bottom + halfSpan;
            }
        }

        finalPos[0] = finalX;
        finalPos[1] = finalY;

        return finalPos;
    }

    protected final float positive(float value) {
        if (value > 0.0f)
            return 1.0f;
        if (value < 0.0f)
            return -1.0f;
        return 0.0f;
    }

    protected final void handleAnimations() {
        long time = AnimationUtils.currentAnimationTimeMillis();

        if (mIsAnimating == false)
            return;

        if (mScaleAnimation.isStopped() && mTranslateAnimation.isStopped()) {
            mIsAnimating = false;
            return;
        } else {
            mView.postInvalidate();
        }

        if (mScaleAnimation.isStopped() == false) {
            mViewScale = mScaleAnimation.getCurrentValue(time);
            if (mCameraListener != null) {
                mCameraListener.onViewScaleChanged(mViewScale);
            }
        }
        if (mTranslateAnimation.isStopped() == false) {
            mViewPos = mTranslateAnimation.getCurrentValues(time);
        }
    }

}

package com.colin.mosaicdemo.art;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * create by colin
 * 2020/11/17
 * <p>
 * 记录手指位置信息的类
 */
public class PointerInfo {

    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_SCALE = 2;
    public static final int TYPE_NONE = 0;
    private float mMaxZoom = 5f;
    private float mMinZoom = 0.3f;
    private int moveType;

    //记录上次手指的点
    private float actionX, actionY;
    private int actionPointerId = MotionEvent.INVALID_POINTER_ID; //单指移动的手指id

    private float spacing; //两指的距离
    private int scalePointerId = MotionEvent.INVALID_POINTER_ID; //第二根手指的id，负责移动和缩放

    private float degree; //两指的角度

    private final Matrix textureMatrix;

    private RectF personDstRect, texturePositionRect;

    private final float safeSpace = 30f;


    public PointerInfo(Matrix textureMatrix) {
        this.textureMatrix = textureMatrix;
    }


    public void setPersonDstRect(RectF personDstRect) {
        this.personDstRect = personDstRect;
    }


    public void setTexturePositionRect(RectF texturePositionRect) {
        this.texturePositionRect = texturePositionRect;
    }

    /**
     * 设置手势类别
     *
     * @param moveType 手势类别
     * @param event    触摸事件
     * @param needSet  需要设置对应值？
     */
    public void setMoveType(int moveType, MotionEvent event, boolean needSet) {
        this.moveType = moveType;
        if (moveType == TYPE_TRANSLATION && needSet) {
            setActionX(event.getX());
            setActionY(event.getY());
        } else if (moveType == TYPE_SCALE && needSet) {
            setSpacing(event);
            setDegree(event);
        }
    }

    /**
     * 计算平移
     *
     * @param event 触摸事件
     */
    public void calculationTranslation(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) == actionPointerId && texturePositionRect != null) {
            //安全区的判断
            float dx = event.getX() - actionX;
            float dy = event.getY() - actionY;

            if (texturePositionRect.right + dx >= personDstRect.left + safeSpace
                    && texturePositionRect.bottom + dy >= personDstRect.top + safeSpace
                    && texturePositionRect.left + dx <= personDstRect.right - safeSpace
                    && texturePositionRect.top + dy <= personDstRect.bottom - safeSpace) {
                textureMatrix.postTranslate(dx, dy);
                setActionX(event.getX());
                setActionY(event.getY());
            }
        }
    }


    /**
     * 计算旋转和缩放
     * <p>
     * 关于旋转中心的问题，旋转中心使用双指距离的中间点体验最好，但由于后续需要保存需要，
     * 不好记录，故使用画布中心点。
     *
     * @param event   触摸事件
     * @param centerX 旋转，缩放中心x坐标
     * @param centerY 旋转，缩放中心y坐标
     */
    public void calculationScaleAndRotation(MotionEvent event, float centerX, float centerY) {
        try {
            if (texturePositionRect.right >= personDstRect.left + safeSpace
                    && texturePositionRect.bottom >= personDstRect.top + safeSpace
                    && texturePositionRect.left <= personDstRect.right - safeSpace
                    && texturePositionRect.top <= personDstRect.bottom - safeSpace) {
                //计算缩放
                float newSpacing = getSpacing(event);
                float newScale = newSpacing / spacing;
                float oldScale = getScale();
                float scale = oldScale * newScale;
//                Log.d("PointerInfo", "spacing = " + spacing + ",newSpacing = "
//                        + newSpacing + ",oldScale = " + oldScale
//                        + ",newScale = " + newScale + ",scale = " + scale);
                if (scale >= mMinZoom && scale <= mMaxZoom) {
                    textureMatrix.postScale(newScale, newScale, centerX, centerY);
                    spacing = newSpacing;
                }

                //计算角度
                float newDegree = getDegree(event);
                float rotate = newDegree - degree;
                textureMatrix.postRotate(rotate, centerX, centerY);
                degree = newDegree;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActionX(float actionX) {
        this.actionX = actionX;
    }

    public void setActionY(float actionY) {
        this.actionY = actionY;
    }

    public int getMoveType() {
        return moveType;
    }

    public int getActionPointerId() {
        return actionPointerId;
    }

    public void setActionPointerId(int actionPointerId) {
        this.actionPointerId = actionPointerId;
    }

    public int getScalePointerId() {
        return scalePointerId;
    }

    public void setScalePointerId(int scalePointerId) {
        this.scalePointerId = scalePointerId;
    }

    public void setSpacing(MotionEvent event) {
        spacing = getSpacing(event);
    }

    public void setDegree(MotionEvent event) {
        degree = getDegree(event);
    }

    public Matrix getTextureMatrix() {
        return textureMatrix;
    }

    // 触碰两点间距离
    private float getSpacing(MotionEvent event) {
        //通过勾股定理得到两点间的距离
        float x = event.getX(findActionIndex(event))
                - event.getX(findSCaleIndex(event));
        float y = event.getY(findActionIndex(event))
                - event.getY(findSCaleIndex(event));
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取旋转角度
    private float getDegree(MotionEvent event) {
        //得到两个手指间的旋转角度
        double delta_x = event.getX(findActionIndex(event)) - event.getX(findSCaleIndex(event));
        double delta_y = event.getY(findActionIndex(event)) - event.getY(findSCaleIndex(event));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private int findActionIndex(MotionEvent event) {
        return event.findPointerIndex(actionPointerId);
    }

    private int findSCaleIndex(MotionEvent event) {
        return event.findPointerIndex(scalePointerId);
    }

    private float getScale() {
        float[] values = new float[9];
        textureMatrix.getValues(values);
        float scalex = values[Matrix.MSCALE_X];
        float skewy = values[Matrix.MSKEW_Y];
        return (float) Math.sqrt(scalex * scalex + skewy * skewy);
    }

    public void setMaxZoom(float maxZoom) {
        this.mMaxZoom = maxZoom;
    }

    public void setMinZoom(float minZoom) {
        this.mMinZoom = minZoom;
    }


}

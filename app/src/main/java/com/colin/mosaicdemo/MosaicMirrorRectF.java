package com.colin.mosaicdemo;

import android.graphics.Rect;
import android.graphics.RectF;

/**
 * create by colin
 * 2020/9/15
 */
public class MosaicMirrorRectF {

    private RectF mRectF;
    private int mRectSize;
    private int mImageRectSize;
    private boolean isLeft = true;
    private int marginTop, marginLeft;
    private int viewWidth;
    //镜子中间的圆的圆心
    private float mCircleX, mCircleY;
    private float mCircleRadius;

    private Rect mImageRect;
    private int imageHeight;
    private int imageWidth;

    public MosaicMirrorRectF(int rectSize, int imageRectSize, int marginTop,
                             int marginLeft, float circleRadius,
                             int viewWidth,
                             int imageWidth, int imageHeight) {
        mRectSize = rectSize;
        mImageRectSize = imageRectSize;
        mRectF = new RectF(marginLeft, marginTop, mRectSize, mRectSize);
        mImageRect = new Rect();
        mCircleRadius = circleRadius;
        this.marginLeft = marginLeft;
        this.marginTop = marginTop;
        this.viewWidth = viewWidth;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
    }


    public void calculate(float viewX, float viewY, float imageX, float imageY) {
        boolean contains = mRectF.contains(viewX, viewY);
        if (contains) {
            if (isLeft) {
                mRectF.set(viewWidth - marginLeft - mRectSize, marginTop,
                        viewWidth - marginLeft, mRectSize + marginTop);
            } else {
                mRectF.set(marginLeft, marginTop, mRectSize, mRectSize + marginTop);
            }
            isLeft = !isLeft;
        }
        mCircleX = mRectF.left + mRectF.width() / 2f;
        mCircleY = mRectF.height() / 2f;

        float tmpLeft = imageX - mImageRectSize / 2f;
        float tmpTop = imageY - mImageRectSize / 2f;
        int left = (int) Math.max(0, tmpLeft);
        int top = (int) Math.max(0, tmpTop);
        int tmpBottom = top + mImageRectSize;
        int tmpRight = left + mImageRectSize;
        int bottom = Math.min(imageHeight, tmpBottom);
        int right = Math.min(imageWidth, tmpRight);
        if (bottom == imageHeight) {
            top = imageHeight - mImageRectSize;
            mCircleY = Math.min(mRectF.bottom - mCircleRadius, mCircleY + (tmpBottom - imageHeight));
        }
        if (right == imageWidth) {
            left = imageWidth - mImageRectSize;
            mCircleX = Math.min(mRectF.right - mCircleRadius, mCircleX + (tmpRight - imageWidth));
        }

        if (top == 0) {
            mCircleY = Math.max(mRectF.top + mCircleRadius, mCircleY - Math.abs(tmpTop));
        }
        if (left == 0) {
            mCircleX = Math.max(mRectF.left + mCircleRadius, mCircleX - Math.abs(tmpLeft));
        }
        mImageRect.set(left, top, right, bottom);
    }


    public RectF getRectF() {
        return mRectF;
    }

    public float getCircleX() {
        return mCircleX;
    }

    public float getCircleY() {
        return mCircleY;
    }

    public Rect getImageRect() {
        return mImageRect;
    }

    public void setImageRectSize(int size) {
        mImageRectSize = size;
    }

    public void setCircleRadius(float radius){
        mCircleRadius = radius;
    }
}

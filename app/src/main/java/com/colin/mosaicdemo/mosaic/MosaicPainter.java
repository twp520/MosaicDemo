package com.colin.mosaicdemo.mosaic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.colin.mosaicdemo.R;
import com.colin.mosaicdemo.util.Logger;
import com.colin.mosaicdemo.util.ValueMappingUtils;
import com.colin.mosaicdemo.util.ViewCamera;

import java.util.ArrayList;

/**
 * create by colin
 * 2020/9/3
 * <p>
 * 马赛克画笔，支持像素块和纹理图片
 * <p>
 * 需求为替换式，即马赛克和纹理二选一。
 */
public class MosaicPainter extends View implements ViewCamera.ViewCameraListener {

    //视图相机
    private ViewCamera mViewCamera;
    //原图
    protected Bitmap mSrcBitmap;
    //结果的图片
    private Bitmap mResultBitmap;
    //用于画路径的透明图片
    protected Bitmap mPathBitmap;

    //普通画笔，叠加
    protected Paint mPaintSrc;
    //用于画路径的画笔
    protected Paint mPaintPath;
    //橡皮擦的画笔
    protected Paint mPaintClean;
    //镜子画笔
    private Paint mPaintMirror;

    //用于路径画布
    protected Canvas mPathCanvas;
    //最终效果画布，通过效果画布滑道resultBitmap上，再把resultBitmap画到view上。
    protected Canvas mResultCanvas;

    //路径集合
    private ArrayList<MosaicPath> mPathList;
    private ArrayList<MosaicPath> mRedoPathList;
    //是否是预览，这时候马赛克还未算出来，不响应事件那些
    private boolean isPreView = true;
    private boolean isInit = false;
    protected boolean isDrawAll = true;
    //是否在拖动缩放
    private boolean mIsTriggerMultiTouch = false;
    //图片距离底部的距离
    protected float mBottomMargin;
    protected Logger logger;
    //当前画笔模式
    public int currentModel;
    //选择的是马赛克还是纹理，
    protected int selectMskModel;
    //回调
    private MosaicInterFace mInterFace;
    private Uri selectedPicUri;

    //镜子相关参数
    private MosaicMirrorRectF mirrorRectF;
    //手指在view上的位置。
    private float mPointerX = -1, mPointerY = -1;
    private float mMirrorCenterCircleRadius; //圆的半径
    private int mMirrorSize;//镜子的大小
    private float mMirrorRoundRadius;

    //画笔大小相关参数
    private float mPaintSize; //画笔大小。
    protected float mFitScale;
    private float mRealPaintSize; //实际画笔大小，需要除以scale
    private int mMinSizeRadius;
    private int mMaxSizeRadius;
    private static final float DEFAULT_SIZE_PERCENT = 30;


    public MosaicPainter(Context context) {
        super(context);
        init();
    }

    public MosaicPainter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mViewCamera = new ViewCamera(this);
        mPathList = new ArrayList<>();
        mRedoPathList = new ArrayList<>();
        mPaintSrc = new Paint(Paint.DITHER_FLAG);
        mPaintSrc.setAntiAlias(true);
        mPaintSrc.setFilterBitmap(true);
        logger = new Logger();
        mPaintPath = new Paint();
        setPathPaint(mPaintPath);

        //纹理的画笔
//        Paint mPaintSRC_IN = new Paint();
//        mPaintSRC_IN.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        mPaintClean = new Paint(mPaintPath);
        mPaintClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        mViewCamera.setViewCameraListener(this);

        mPaintMirror = new Paint(Paint.DITHER_FLAG);
        mPaintMirror.setStyle(Paint.Style.STROKE);
        mPaintMirror.setStrokeWidth(8f);
        mPaintMirror.setShadowLayer(4f, 0f, 0f, Color.BLACK);
        mPaintMirror.setColor(Color.WHITE);
        mPaintMirror.setAntiAlias(true);

        mMirrorSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                128, getResources().getDisplayMetrics());
        mMirrorRoundRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5, getResources().getDisplayMetrics());
        mMinSizeRadius = getContext().getResources().getDimensionPixelSize(R.dimen.min_size);

        mMaxSizeRadius = getContext().getResources().getDimensionPixelSize(R.dimen.max_size);

        mRealPaintSize = mPaintSize = ValueMappingUtils.getLinearOutput(0, mMinSizeRadius,
                100, mMaxSizeRadius, DEFAULT_SIZE_PERCENT);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSrcBitmap == null) {
            logger.warning("no bitmap");
            return;
        }
        mViewCamera.onDrawInit(canvas, mBottomMargin);
        //图像中心为绘制原点
        float x = -mSrcBitmap.getWidth() / 2.0f;
        float y = -mSrcBitmap.getHeight() / 2.0f;
        if (!isInit || isPreView || isEmptyEraserModel()) {
            canvas.drawBitmap(mSrcBitmap, x, y, mPaintSrc);
            mViewCamera.onDrawEnd(canvas);
        } else {
            mResultCanvas.drawBitmap(mSrcBitmap, 0, 0, mPaintSrc);
            mResultCanvas.save();
            drawMask();
            drawPathToCanvas();
            mResultCanvas.restore();
            canvas.drawBitmap(mResultBitmap, x, y, mPaintSrc);
            mViewCamera.onDrawEnd(canvas);
            //画复制镜, 复制镜不需要进行放大变换，使用view原始canvas即可。
            if (mPointerX > -1 && mPointerY > -1 && !mIsTriggerMultiTouch) {
                float[] imagePosition = mViewCamera.mapToImageFromView(mPointerX, mPointerY);
                mirrorRectF.calculate(mPointerX, mPointerY, imagePosition[0], imagePosition[1]);
                if (!mirrorRectF.circleOverEdge) { // 超出边界不显示指示镜
                    canvas.save();
                    canvas.drawBitmap(mResultBitmap, mirrorRectF.getImageRect(), mirrorRectF.getRectF(), null);
                    mPaintMirror.setStrokeWidth(8f);
                    canvas.drawRoundRect(mirrorRectF.getRectF(), mMirrorRoundRadius, mMirrorRoundRadius, mPaintMirror);
                    mPaintMirror.setStrokeWidth(4f);
                    canvas.drawCircle(mirrorRectF.getCircleX(), mirrorRectF.getCircleY(),
                            mMirrorCenterCircleRadius, mPaintMirror);
                    canvas.restore();
                }
            }
        }
    }

    protected void drawMask() {
        if (isDrawAll) {
            mPathCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //对path进行合成
            preDrawAll(mPathCanvas, mPaintPath, true);
            for (MosaicPath mosaicPath : mPathList) {
                drawSinglePath(mosaicPath, mPathCanvas);
            }
            isDrawAll = false;
        } else {
            if (!mPathList.isEmpty()) {
                MosaicPath lastPath = getLastPath();
                drawSinglePath(lastPath, mPathCanvas);
            }
        }
    }

    protected void preDrawAll(Canvas pathCanvas, Paint pathPaint, boolean needBlur) {

    }

    protected void drawPathToCanvas() {
        mResultCanvas.drawBitmap(mPathBitmap, 0, 0, mPaintSrc);
    }

    protected void setPathPaint(Paint pathPaint) {
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setColor(Color.RED);
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
    }

    protected void drawSinglePath(MosaicPath lastPath, Canvas pathCanvas) {
        /*if (lastPath.type == MosaicPath.TYPE_CLEAN) {
            mPaintClean.setStrokeWidth(lastPath.size);
            pathCanvas.drawPath(lastPath.path, mPaintClean);
        } else {
            logger.info("drawSinglePath size = " + lastPath.size);
            mPaintPath.setStrokeWidth(lastPath.size);
            pathCanvas.drawPath(lastPath.path, mPaintPath);
        }*/
        drawSinglePath(lastPath, pathCanvas, mPaintClean, mPaintPath);
    }

    public static void drawSinglePath(MosaicPath lastPath, Canvas pathCanvas
            , Paint paintClean, Paint paintPath) {
        if (lastPath.type == MosaicPath.TYPE_CLEAN) {
            paintClean.setStrokeWidth(lastPath.size);
            pathCanvas.drawPath(lastPath.path, paintClean);
        } else {
            paintPath.setStrokeWidth(lastPath.size);
            pathCanvas.drawPath(lastPath.path, paintPath);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mSrcBitmap == null) {
            return false;
        }
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN && mInterFace != null) {
            mInterFace.onTouchDown();
        } else if (action == MotionEvent.ACTION_UP && mInterFace != null) {
            mInterFace.onTouchUp();
        }

        if (mViewCamera.onTouchEvent(event)) {
            mIsTriggerMultiTouch = true;
            return true;
        }
        //还在preview的时候不做操作
        if (!isInit || isPreView)
            return true;

        if (isEmptyEraserModel())
            return true;

        float[] posImage = mViewCamera.mapToImageFromView(event.getX(),
                event.getY());
        //进行拖动，创建path添加。
        if (action == MotionEvent.ACTION_DOWN) {
            mIsTriggerMultiTouch = false;
            MosaicPath path = createPath();
            path.path.moveTo(posImage[0], posImage[1]);
            mPointerX = event.getX();
            mPointerY = event.getY();
            invalidate();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mIsTriggerMultiTouch)
                return true;
            getLastPath().path.lineTo(posImage[0], posImage[1]);
            mPointerX = event.getX();
            mPointerY = event.getY();
            invalidate();
        } else if (action == MotionEvent.ACTION_UP) {
            mPointerX = -1;
            mPointerY = -1;
            invalidate();
            if (mInterFace != null)
                mInterFace.onMosaicDrawComplete(mPathList.size());
            logger.info("touch up, path size = " + mPathList.size());
        }
        return true;
    }

    protected boolean isEmptyEraserModel() {
        return mPathList.isEmpty() && currentModel == MosaicPath.TYPE_CLEAN;
    }

    private MosaicPath getLastPath() {
        if (mPathList.isEmpty()) {
            return createPath();
        }
        return mPathList.get(mPathList.size() - 1);
    }

    private MosaicPath createPath() {
        MosaicPath path = new MosaicPath();
        path.type = currentModel;
        path.path = new Path();
        path.size = mRealPaintSize;
        mPathList.add(path);
        return path;
    }

    /**
     * 一开始的时候设置原图，给用户预览
     *
     * @param src 原图
     */
    public void setPreView(Bitmap src) {
        mSrcBitmap = src;
        mViewCamera.setImageSize(mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
    }

    /**
     * 设置是否预览
     *
     * @param preView 是否预览
     */
    public void setPreView(boolean preView) {
        isPreView = preView;
        invalidate();
    }

    /**
     * 初始化方法，必须调用。
     *
     * @param src 原图
     * @param msk 像素马赛克化后的图片
     */
    public void setBitmaps(Bitmap src, Bitmap msk, boolean needShader) {
        mSrcBitmap = src;
        mPathBitmap = Glide.get(getContext()).getBitmapPool()
                .get(mSrcBitmap.getWidth(), mSrcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mResultBitmap = mPathBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mPathCanvas = new Canvas(mPathBitmap);
        mResultCanvas = new Canvas(mResultBitmap);
        mViewCamera.setBlock(true);
        mViewCamera.setImageSize(mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
        mFitScale = mViewCamera.getViewScale();
        mirrorRectF = new MosaicMirrorRectF(mMirrorSize, mMirrorSize, 10, 10,
                mMirrorCenterCircleRadius, getWidth(), mSrcBitmap.getWidth(), mSrcBitmap.getHeight());
        setRealPaintSizeAndMirrorCenterCircleRadius();
        isInit = true;
        isPreView = false;
        setMskBitmap(msk, needShader);
    }


    /**
     * 设置为马赛克模式
     */
    public void setMskBitmap(Bitmap msk, boolean needShader) {
        //整张图被算法像素马赛克化后的图片
        if (needShader)
            mPaintPath.setShader(new BitmapShader(msk, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        currentModel = MosaicPath.TYPE_SMUDGE;
        selectMskModel = MosaicPath.TYPE_SMUDGE;
        isDrawAll = true;
        invalidate();
    }


    public void setBottomMargin(float bottomMargin) {
        this.mBottomMargin = bottomMargin;
        invalidate();
    }


    /**
     * 设置为纹理模式
     *
     * @param texture 纹理素材
     */
    public void setPicModel(Bitmap texture, Uri uri) {
        if (currentModel == MosaicPath.TYPE_PIC && uri.equals(selectedPicUri))
            return;
        currentModel = MosaicPath.TYPE_PIC;
        selectMskModel = MosaicPath.TYPE_PIC;
        selectedPicUri = uri;
        mPaintPath.setShader(new BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        isDrawAll = true;
        invalidate();
    }

    /**
     * 设置为橡皮擦模式
     */
    public void setCleanModel() {
        if (currentModel == MosaicPath.TYPE_CLEAN)
            return;
        currentModel = MosaicPath.TYPE_CLEAN;
    }

    public void setMosaicInterFace(MosaicInterFace mInterFace) {
        this.mInterFace = mInterFace;
    }


    public ViewCamera getViewCamera() {
        return mViewCamera;
    }

    public int getSelectMskModel() {
        return selectMskModel;
    }


    @Override
    public void onViewScaleChanged(float viewScale) {
        if (!isInit)
            return;
        float scale = viewScale / mFitScale;
        float tmpMirrorImageSize = mMirrorSize / scale;
        mirrorRectF.setImageRectSize((int) tmpMirrorImageSize);
        mRealPaintSize = mPaintSize / scale;
        logger.info("onViewScaleChanged scale = " + viewScale);
    }

    //undo时候，将pathList的最后一条移除，添加到redoList去
    public void undo() {
        if (mPathList.isEmpty())
            return;
        MosaicPath remove = mPathList.remove(mPathList.size() - 1);
        mRedoPathList.add(remove);
        isDrawAll = true;
        invalidate();
        if (mInterFace != null) {
            mInterFace.onUndoComplete(mPathList.size(), mRedoPathList.size());
        }
    }

    //redo将 redoList的最后一条移除，添加到pathList中去
    public void redo() {
        if (mRedoPathList.isEmpty())
            return;
        MosaicPath remove = mRedoPathList.remove(mRedoPathList.size() - 1);
        mPathList.add(remove);
        isDrawAll = true;
        invalidate();
        if (mInterFace != null) {
            mInterFace.onRedoComplete(mPathList.size(), mRedoPathList.size());
        }
    }

    public void clearRedo() {
        mRedoPathList.clear();
    }

    public int getDefaultDisplaySizeProgress() {
        return (int) DEFAULT_SIZE_PERCENT;
    }

    public int getDisplaySizeProgress() {
        return (int) (ValueMappingUtils.getLinearOutput(mMinSizeRadius, 0,
                mMaxSizeRadius, 100, mPaintSize));
    }

    public void setPaintSizeByProgress(int progress) {
        mPaintSize = ValueMappingUtils.getLinearOutput(0, mMinSizeRadius,
                100, mMaxSizeRadius, progress);
        setRealPaintSizeAndMirrorCenterCircleRadius();
    }

    private void setRealPaintSizeAndMirrorCenterCircleRadius() {
        float scale = mViewCamera.getViewScale() / mFitScale;
        mRealPaintSize = mPaintSize / scale;
        mMirrorCenterCircleRadius = mPaintSize / 2f;
        if (mirrorRectF != null)
            mirrorRectF.setCircleRadius(mMirrorCenterCircleRadius);
    }

    public void reset(boolean invalidate) {
        mPathList.clear();
        mRedoPathList.clear();
        isDrawAll = true;
        if (invalidate) {
            invalidate();
        }
    }

    public void cleanBitmap() {
        if (mSrcBitmap != null) {
            mSrcBitmap.recycle();
        }
        if (mPathBitmap != null) {
            mPathBitmap.recycle();
        }
        if (mResultBitmap != null) {
            mResultBitmap.recycle();
        }
        if (mPathCanvas != null) {
            mPathCanvas.setBitmap(null);
        }
        if (mResultCanvas != null) {
            mResultCanvas.setBitmap(null);
        }
        mPathList.clear();
        mRedoPathList.clear();
    }

    public float getPaintSize() {
        return mPaintSize;
    }

    public Bitmap getResultBitmap() {
        return mResultBitmap;
    }

    public ArrayList<MosaicPath> getPathList() {
        return mPathList;
    }


    public interface MosaicInterFace {

        void onTouchDown();

        void onTouchUp();

        void onMosaicDrawComplete(int pathSize);

        void onUndoComplete(int pathSize, int redoSize);

        void onRedoComplete(int pathSize, int redoSize);
    }

}

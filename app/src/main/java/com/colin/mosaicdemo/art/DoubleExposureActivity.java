package com.colin.mosaicdemo.art;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.colin.mosaicdemo.databinding.ActivityDoubleExposureBinding;

import java.io.IOException;

/**
 * create by colin
 * 2020/12/10
 */
public class DoubleExposureActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityDoubleExposureBinding viewBinding;
    private Bitmap expTextureBitmap;
    private Bitmap srcBitmap;
    private Path personPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityDoubleExposureBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        //初始化素材
        try {
            expTextureBitmap = BitmapFactory.decodeStream(getAssets().open("abstract_pattern.jpg"));
            srcBitmap = BitmapFactory.decodeStream(getAssets().open("src.jpg"));
            Bitmap blackBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            Canvas canvas = new Canvas(blackBitmap);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setDither(true);
            paint.setAntiAlias(true);
            paint.setColorFilter(filter);
            canvas.drawBitmap(srcBitmap, 0, 0, paint);

            //这里最好使用人像识别出的区域，演示为了方便直接添加为所有区域。
            personPath = new Path();
            personPath.addRect(new RectF(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()),
                    Path.Direction.CCW);
            viewBinding.viewPainter.setMskModel();
            viewBinding.viewPainter.getViewCamera().setImageSize(srcBitmap.getWidth(), srcBitmap.getHeight());
            viewBinding.viewPainter.setBlackSrcBitmap(blackBitmap);

            viewBinding.viewPainter.post(new Runnable() {
                @Override
                public void run() {
                    setBitmap();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setBitmap() {
        viewBinding.viewPainter.setPersonAndBitmaps(srcBitmap, personPath, -1f);
        viewBinding.viewExposure.setBitmaps(
                viewBinding.viewPainter.createPersonBitmap(true, false)
                , expTextureBitmap);

        viewBinding.viewPainter.setVisibility(View.GONE);

        viewBinding.buttonLeft.setOnClickListener(this);
        viewBinding.buttonRight.setOnClickListener(this);
        viewBinding.btnEra.setOnClickListener(this);
        viewBinding.btnPaint.setOnClickListener(this);
        viewBinding.btnUndo.setOnClickListener(this);
        viewBinding.btnRedo.setOnClickListener(this);
        viewBinding.btnReset.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v == viewBinding.buttonLeft) {
            viewBinding.viewPainter.createPersonBitmap(true,false);
            viewBinding.viewPainter.setVisibility(View.GONE);
            viewBinding.viewExposure.setVisibility(View.VISIBLE);
            viewBinding.layoutPainter.setVisibility(View.INVISIBLE);
        } else if (v == viewBinding.buttonRight) {
            viewBinding.viewExposure.setVisibility(View.GONE);
            viewBinding.viewPainter.setVisibility(View.VISIBLE);
            viewBinding.layoutPainter.setVisibility(View.VISIBLE);
        } else if (v == viewBinding.btnReset) {
            viewBinding.viewPainter.reset(true);
        } else if (v == viewBinding.btnUndo) {
            viewBinding.viewPainter.undo();
        } else if (v == viewBinding.btnRedo) {
            viewBinding.viewPainter.redo();
        } else if (v == viewBinding.btnPaint) {
            viewBinding.viewPainter.setMskModel();
            viewBinding.tvInfo.setText("当前选中画笔模式");
        } else if (v == viewBinding.btnEra) {
            viewBinding.viewPainter.setCleanModel();
            viewBinding.tvInfo.setText("当前选中橡皮擦模式");
        }

    }
}

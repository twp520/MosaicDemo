package com.colin.mosaicdemo.mosaic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.colin.mosaicdemo.R;
import com.colin.mosaicdemo.util.SizeUtils;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements MosaicPainter.MosaicInterFace {

    private MosaicPainter mosaicPainter;
    private Bitmap srcBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mosaicPainter = findViewById(R.id.main_mp);
        mosaicPainter.setMosaicInterFace(this);
        //加载原图
        try {
            InputStream open = getAssets().open("src.jpg");
            srcBitmap = BitmapFactory.decodeStream(open);
            mosaicPainter.post(new Runnable() {
                @Override
                public void run() {
                    int botMargin = getResources().getDimensionPixelSize(R.dimen.bot_height);
                    mosaicPainter.setBottomMargin(botMargin);
                    float fitScale = SizeUtils.computeFitScale(srcBitmap,
                            mosaicPainter.getWidth(), mosaicPainter.getHeight() - botMargin);
                    mosaicPainter.getViewCamera().setViewScale(fitScale, fitScale, fitScale * 2f);
                    mosaicPainter.getViewCamera().setImageSize(srcBitmap.getWidth(), srcBitmap.getHeight());
                    //这里马赛克化的图片需要有算法生成，方便演示。使用原图代替。
                    mosaicPainter.setBitmaps(srcBitmap, srcBitmap, true);
                    setPic();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPic() {
        final Uri textureUri = Uri.parse("file:///android_asset/pic1.jpg");
        Glide.with(this)
                .asBitmap()
                .load(textureUri)
                .optionalCenterCrop()
                .into(new CustomTarget<Bitmap>(srcBitmap.getWidth(), srcBitmap.getHeight()) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        mosaicPainter.setPicModel(resource, textureUri);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    public void undo(View view) {
        mosaicPainter.undo();
    }

    public void redo(View view) {
        mosaicPainter.redo();
    }

    public void eraser(View view) {
        mosaicPainter.setCleanModel();
    }

    public void save(View view) {
        //todo
        Bitmap resultBitmap = mosaicPainter.getResultBitmap();
    }

    public void texture(View view) {
        setPic();
    }




    @Override
    public void onTouchDown() {

    }

    @Override
    public void onTouchUp() {

    }

    @Override
    public void onMosaicDrawComplete(int pathSize) {

    }

    @Override
    public void onUndoComplete(int pathSize, int redoSize) {

    }

    @Override
    public void onRedoComplete(int pathSize, int redoSize) {

    }
}
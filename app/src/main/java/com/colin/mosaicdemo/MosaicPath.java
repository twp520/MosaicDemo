package com.colin.mosaicdemo;

import android.graphics.Path;

/**
 * create by colin
 * 2020/9/15
 */
public class MosaicPath {
    public static int TYPE_PIC = 1;  //图片纹理路径。
    public static int TYPE_SMUDGE = 2; //像素块路径。
    public static int TYPE_CLEAN = 3; //橡皮擦
    public Path path; //路径
    public int type; //类型
    public float size;//画笔大小
}

package com.colin.mosaicdemo;


public class ValueMappingUtils {

    /**
     * 已知一个点的横坐标，求他在直线上的纵坐标
     *
     * @param x0    第一个点的横坐标
     * @param y0    第一个点的纵坐标
     * @param x1    第二个点的横坐标
     * @param y1    第二个点的纵坐标
     * @param input 要求的的点的横坐标
     * @return 要求的的点的纵坐标
     */
    public static float getLinearOutput(float x0, float y0, float x1, float y1, float input) {
        if (x1 == x0) {
            throw new IllegalArgumentException("x0 mustn't be equal to x1");
        }
        float k = (y1 - y0) / (x1 - x0);
        return k * (input - x0) + y0;
    }


    /**
     * 已知一个点的横坐标，求他在折线上的纵坐标
     *
     * @param x0    第一个点的横坐标
     * @param y0    第一个点的纵坐标
     * @param x1    中间点的横坐标
     * @param y1    中间点的纵坐标
     * @param x2    第二个点的横坐标
     * @param y2    第二个点的纵坐标
     * @param input 要求的的点的横坐标
     * @return 要求的的点的纵坐标
     */
    public static float getLinearOutput(float x0, float y0, float x1, float y1, float x2, float y2, float input) {
        if (x0 <= x1) {
            if (input <= x1) {
                return getLinearOutput(x0, y0, x1, y1, input);
            } else {
                return getLinearOutput(x1, y1, x2, y2, input);
            }
        } else {
            if (input <= x1) {
                return getLinearOutput(x1, y1, x2, y2, input);
            } else {
                return getLinearOutput(x0, y0, x1, y1, input);
            }
        }

    }
}

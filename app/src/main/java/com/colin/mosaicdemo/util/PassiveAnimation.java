package com.colin.mosaicdemo.util;

import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * 
 * @author wxfred
 *
 */
public class PassiveAnimation {
	
	@SuppressWarnings("unused")
	private static final String TAG = "PassiveAnimation";
	
	private Interpolator interpolator = null;
	private long startTime;
	private long duration;
	private float[] startValues;
	private float[] endValues;
	private boolean stopped;
	
	public PassiveAnimation() {
		interpolator = new LinearInterpolator();
		startTime = 0;
		duration = 200;
		startValues = new float[1];
		endValues = new float[1];
		stopped = true;
	}
	
	// Getter(s)
	public float getStartValue() {
		return startValues[0];
	}
	
	public float[] getStartValues() {
		return startValues.clone();
	}
	
	public float getEndValue() {
		return endValues[0];
	}
	
	public float[] getEndValues() {
		return endValues.clone();
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	// Setter(s)
	public void setInterpolator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}
	
	public void setStartValue(float startValue) {
		this.startValues = new float[1];
		this.startValues[0] = startValue;
	}
	
	public void setStartValues(float[] startValues) {
		this.startValues = startValues.clone();
	}
	
	public void setEndValue(float endValue) {
		this.endValues = new float[1];
		this.endValues[0] = endValue;
	}
	
	public void setEndValues(float[] endValues) {
		this.endValues = endValues.clone();
	}
	
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	// Public method(s)
	public void start() {
		startTime = AnimationUtils.currentAnimationTimeMillis();
		stopped = false;
	}
	
	public void stop() {
		stopped = true;
	}
	
	/**
	 * @return current interpolation
	 */
	public float getCurrentInterpolation() {
		long time = AnimationUtils.currentAnimationTimeMillis();
		return getInterpolation(time);
	}
	
	/**
	 * @return current value
	 */
	public float getCurrentValue() {
		float interpolation = getCurrentInterpolation();
		return calculateValue(interpolation, 0);
	}
	
	public float[] getCurrentValues() {
		float interpolation = getCurrentInterpolation();
		return calculateValues(interpolation);
	}
	
	public float getCurrentValue(long time) {
		float interpolation = getInterpolation(time);
		return calculateValue(interpolation, 0);
	}
	
	public float[] getCurrentValues(long time) {
		float interpolation = getInterpolation(time);
		return calculateValues(interpolation);
	}

	// Protected method(s)
	protected float getInterpolation(long time) {
		// TODO: 会不会超界？
		long progress = time - startTime;
		if (progress > duration) {
			stopped = true;
			return 1.0f;
		}
		return interpolator.getInterpolation((float)progress / duration);
	}
	
	protected float calculateValue(float interpolation, int num) {
		return startValues[num] + (endValues[num] - startValues[num]) * interpolation;
	}
	
	protected float[] calculateValues(float interpolation) {
		int length = startValues.length;
		float[] values = new float[length];
		for (int i=0;i<length;i++) values[i] = calculateValue(interpolation, i);
		return values;
	}
}

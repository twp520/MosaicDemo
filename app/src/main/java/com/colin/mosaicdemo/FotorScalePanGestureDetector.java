package com.colin.mosaicdemo;

import android.view.MotionEvent;

/**
 * Gesture detector for multi-pointer scale and pan.
 * 
 * <li>When pointer count reaches the minimum of touch pointer (default is 2) , {@link OnFotorScalePanGestureListener#onScalePanFocus(float, float)} will be invoked.</li>
 * <li>After the begin of gesture, any pointer's motion will active {@link OnFotorScalePanGestureListener#onScalePan(float, float, float)}.</li>
 * <li>The pointers of which the index is larger than maximum of touch pointer will be ignore.</li>
 * <li>Add or release pointers in the range of minimum and maximum of touch pointer will relocate (or restart) gesture.</li>
 * <li>If current pointer count is greater than maximum of touch pointer, the release of pointers which is valid will force the detector to quit current gesture (user need to release all pointers to restart gesture).</li>
 * <li>When current pointer count is equal to minimum of touch pointer, the releasing will call {@link OnFotorScalePanGestureListener#onScalePanEnd()}.</li>
 * 
 * @author wxfred
 */
public class FotorScalePanGestureDetector {
	
	public interface OnFotorScalePanGestureListener {
		/**
		 * The begin of scale and pan gesture.
		 * Return current focus point.
		 * 
		 * @param focusX The X coordinate of focus point
		 * @param focusY The Y coordinate of focus point
		 */
		public void onScalePanFocus(float focusX, float focusY);
		
		/**
		 * Return the scaling factor from the previous scale event to the current event,
		 * and translation vector from previous focus point to current focus point.
		 * 
		 * @param scaleFactor The current scale factor
		 * @param panX The X axis shift
		 * @param panY The Y axis shift
		 */
		public void onScalePan(float scaleFactor, float panX, float panY);
		
		/**
		 * Responds to the end of a scale and pan gesture.
		 */
		public void onScalePanEnd();
	}
	
	//private static final String TAG = "FotorScalePanGestureDetector";
	
	private OnFotorScalePanGestureListener mListener;
	private int mTouchPointerMin, mTouchPointerMax;
	private float mPreFocusX, mPreFocusY;
	private float mPreSpan;
	private boolean mWaitingToQuit;
	
	public FotorScalePanGestureDetector(OnFotorScalePanGestureListener listener) {
		mListener = listener;
		mTouchPointerMin = 2;
		mTouchPointerMax = Integer.MAX_VALUE;
		mWaitingToQuit = false;
	}
	
	/**
	 * Accepts MotionEvents and dispatches events to a {@link OnFotorScalePanGestureListener} when appropriate.
	 * 
	 * @return True if the event was handled, false otherwise.
	 */
	public boolean onTouchEvent(MotionEvent event) {
		int count = event.getPointerCount();
		int action = event.getActionMasked();
		if (mWaitingToQuit) {
			if (action == MotionEvent.ACTION_UP) mWaitingToQuit = false;
			return true;
		}
		// Pointer count should greater than (or equal to) minimum touch pointer
		if (count < mTouchPointerMin) return false;
		
		boolean reset = false;
		int actionIndex = event.getActionIndex();
		int skipIndex = -1;
		int div = count;
		if (action == MotionEvent.ACTION_POINTER_DOWN) {
			if (count <= mTouchPointerMax) {
				reset = true;
			}
		}
		if (action == MotionEvent.ACTION_POINTER_UP) {
			if (count == mTouchPointerMin) {
				mListener.onScalePanEnd();
				return true;
			}
			if (actionIndex < mTouchPointerMax && count > mTouchPointerMax) {
				mWaitingToQuit = true;
				mListener.onScalePanEnd();
				return true;
			}
			if (count > mTouchPointerMax) return true;
			reset = true;
			skipIndex = actionIndex;
			div--;
		}
		
		if (count > mTouchPointerMax) {
			count = mTouchPointerMax;
			div = count;
		}
		
		// Calculate focal point of pointers
		float focusX = 0.0f;
		float focusY = 0.0f;
		for (int i=0;i<count;i++) {
			if (i == skipIndex) continue;
			focusX += event.getX(i);
			focusY += event.getY(i);
		}
		focusX /= div;
		focusY /= div;
		
		// Determine average deviation from focal point
        float devX = 0.0f;
        float devY = 0.0f;
        for (int i=0;i<count;i++) {
            if (i == skipIndex) continue;
            devX += Math.abs(event.getX(i) - focusX);
            devY += Math.abs(event.getY(i) - focusY);
        }
        devX /= div;
        devY /= div;
		
        // Span is the diameter of the circle which is combined with touch pointers and of which the center is the focal point
        float spanX = devX * 2.0f;
        float spanY = devY * 2.0f;
		float span = (float) Math.sqrt(spanX * spanX + spanY * spanY);
		
		// Reset focal point
		if (reset) {
			mPreFocusX = focusX;
			mPreFocusY = focusY;
			mPreSpan = span;
			mListener.onScalePanFocus(mPreFocusX, mPreFocusY);
			return true;
		}
		
		// Scale and pan
		float scaleFactor = span / mPreSpan;
		float panX = focusX - mPreFocusX;
		float panY = focusY - mPreFocusY;
		mListener.onScalePan(scaleFactor, panX, panY);
		
		return true;
	}

	// Getter(s)
	public int getTouchPointerMin() {
		return mTouchPointerMin;
	}

	public int getTouchPointerMax() {
		return mTouchPointerMax;
	}

	// Setter(s)
	public void setTouchPointerMin(int min) {
		mTouchPointerMin = min;
	}

	public void setTouchPointerMax(int max) {
		mTouchPointerMax = max;
	}
	
	/**
	 * Minimum and maximum of touch pointer are both 2.
	 */
	public void setToTwoPointerTouch() {
		mTouchPointerMin = 2;
		mTouchPointerMax = 2;
	}
	
	/**
	 * Default setting, minimum of touch pointer is 2.
	 */
	public void setToMultiPointerTouch() {
		mTouchPointerMin = 2;
		mTouchPointerMax = Integer.MAX_VALUE;
	}
	
}

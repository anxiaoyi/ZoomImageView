package com.zk.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener, OnTouchListener {

	static final String TAG = ZoomImageView.class.getSimpleName();
	
	private boolean mOnce;
	private Matrix mScaleMatrix;

	private float mInitScale;
	private float mCurScale;
	private float mMaxScale;
	private ScaleGestureDetector mScaleGestureDetector;
	
	/** last pointer count **/
	private int mLastPointerCount;
	private float mLastX;
	private float mLastY;
	private float mTouchSlop;
	private boolean isCanDrag;
	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;
	
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;
	
	public ZoomImageView(Context context) {
		super(context);
		init();
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init(){
		mScaleMatrix = new Matrix();
		setScaleType(ScaleType.MATRIX);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
			@Override
			public void onScaleEnd(ScaleGestureDetector arg0) {
				
			}
			
			@Override
			public boolean onScaleBegin(ScaleGestureDetector arg0) {
				return true;
			}
			
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				if(getDrawable() == null){
					return true;
				}
				
				float scale = getScale();
				float scaleFactor = detector.getScaleFactor();
				
				Log.d(TAG, String.format("[scaleFactor:%f, curScale:%f]", scaleFactor, scale));
				
				if(scale < mMaxScale && scaleFactor > 1.0F || 
						(scale > mInitScale && scaleFactor < 1.0F)){
					if(scaleFactor * scale < mInitScale){
						scaleFactor = mInitScale / scale;
					}
					
					if(scaleFactor * scale > mMaxScale){
						scaleFactor = mMaxScale / scale;
					}
					
					//scale
					mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
					
					//check border to prevent show white border
					checkBorderAndCenterWhenScale();
					
					setImageMatrix(mScaleMatrix);
				}
				
				return true;
			}
		});
	
		mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if(isAutoScale){
					return true;
				}
				
				float x = e.getX();
				float y = e.getY();
				
				if(getScale() < mCurScale){
					postDelayed(new AudoScaleRunnable(mCurScale, x, y), 16);
					isAutoScale = true;
				}else{
					postDelayed(new AudoScaleRunnable(mInitScale, x, y), 16);
					isAutoScale = true;
				}
				
				return true;
			}
		});
	}

	/**
	 * Control boarder and position corrent when scale image
	 */
	private void checkBorderAndCenterWhenScale() {
		RectF rect = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;
		int width = getWidth();
		int height = getHeight();
		
		if(rect.width() >= width){
			if(rect.left > 0){
				deltaX = -rect.left;
			}
			
			if(rect.right < width){
				deltaX = width - rect.right;
			}
		}
		
		if(rect.height() >= height){
			if(rect.top > 0){
				deltaY = -rect.top;
			}
			
			if(rect.bottom < height){
				deltaY = height - rect.bottom;
			}
		}
		
		//If width or height smaller than view's width and view's height, then center it
		if(rect.width() < width){
			deltaX = width / 2.0F - rect.right + rect.width() / 2.0F;
		}
		
		if(rect.height() < height){
			deltaY = height / 2.0F - rect.bottom + rect.height() / 2.0F;
		}
		
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}
	
	/**
	 * 得到放大或者缩小之后的图片Left,Right,Top,Bottom
	 * @return
	 */
	private RectF getMatrixRectF() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		
		Drawable d = getDrawable();
		if(d != null){
			rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}
		
		return rectF;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
		setOnTouchListener(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeOnGlobalLayoutListener(this);
	}
	
	/**
	 * Get current scale
	 * @return
	 */
	private float getScale(){
		float values[] = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	@Override
	public void onGlobalLayout() {
		if(!mOnce){
			
			//get view's width and height
			int width = getWidth();
			int height = getHeight();
			
			//get image drawable
			Drawable drawable = getDrawable();
			if(drawable == null){
				return;
			}
			
			int drawableWidth = drawable.getIntrinsicWidth();
			int drawableHeight = drawable.getIntrinsicHeight();
			
			float scale = 1.0F;
			//The image is very horizontal large
			if(drawableWidth > width && drawableHeight < height){
				Log.d(TAG, String.format("DrawableWidth > ViewWidth"));
				scale = width * 1.0F / drawableWidth;
			}
			
			//The image is very vertical large
			if(drawableHeight > height && drawableWidth < width){
				Log.d(TAG, String.format("DrawableHeight > viewHeight"));
				scale = height * 1.0F / drawableHeight;
			}
			
			//The image is horizontal and vertical are larger than screen
			if((drawableWidth > width && drawableHeight > height) || 
					(drawableWidth < width && drawableHeight < height)){
				Log.d(TAG, String.format("DrawableWidth > width && DrawableHeight > height || < && <"));
				scale = Math.min(width * 1.0F / drawableWidth, height * 1.0F / drawableHeight);
			}
			
			mInitScale = scale;
			mMaxScale = scale * 4;
			mCurScale = scale * 2;
			
			Log.d(TAG, String.format("[initScale:%f, curScale:%f, maxScale:%f]", mInitScale, mCurScale, mMaxScale));
			
			//Move Image to View's center
			//-----------------------
			//|		|				|
			//|		|				|
			//|		|				|
			//-------				|
			//						|
			//						|
			//----------------------|
			
			int dx = (width - drawableWidth) / 2;
			int dy = (height - drawableHeight) / 2;
			
			//ImageMatrix
			//xScale, ySkew, xTrans
			//ySkew, yScale, yTrans
			//0,	0,		0
			mScaleMatrix.postTranslate(dx, dy);
			mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
			setImageMatrix(mScaleMatrix);
			
			mOnce = true;
		}
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		if(mGestureDetector.onTouchEvent(event)){
			return true;
		}
		
		mScaleGestureDetector.onTouchEvent(event);
		moveGestureDetector(event);
		return true;
	}

	/**
	 * Detect move event
	 * @param event
	 */
	private void moveGestureDetector(MotionEvent event) {
		float x = 0;
		float y = 0;
		
		int pointerCount = event.getPointerCount();
		for(int i=0; i<pointerCount; ++i){
			x += event.getX(i);
			y += event.getY(i);
		}
		
		x /= pointerCount;
		y /= pointerCount;
		
		if(pointerCount != mLastPointerCount){
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		
		mLastPointerCount = pointerCount;
		RectF rect = getMatrixRectF();
		
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			checkShouldAllowParentInterceptTouchEvent(rect);
			break;
		
		case MotionEvent.ACTION_MOVE:
			checkShouldAllowParentInterceptTouchEvent(rect);			
			float dx = x - mLastX;
			float dy = y - mLastY;
			
			if(!isCanDrag){
				isCanDrag = isMoveAction(dx, dy);
			}
			
			if(isCanDrag){
				RectF rectf = getMatrixRectF();
				if(getDrawable() != null){
					
					isCheckLeftAndRight = true;
					isCheckTopAndBottom = true;
					
					if(rectf.width() < getWidth()){
						isCheckLeftAndRight = false;
						dx = 0;
					}
					
					if(rectf.height() < getHeight()){
						isCheckTopAndBottom = false;
						dy = 0;
					}
					
					mScaleMatrix.postTranslate(dx, dy);
					checkBorderWhenTranslate();
					setImageMatrix(mScaleMatrix);
				}
			}
			mLastX = x;
			mLastY = y;
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mLastPointerCount = 0;
			break;
		}
	}

	private void checkShouldAllowParentInterceptTouchEvent(RectF rect) {
		if(getParent() != null && getParent() instanceof ViewPager){
			if(rect.width() > getWidth() + 0.01 ||
					(rect.height() > getHeight() + 0.01)){
				//Disallow to intercept our touch event
				getParent().requestDisallowInterceptTouchEvent(true);
			}
		}	
	}

	private void checkBorderWhenTranslate() {
		RectF rectF = getMatrixRectF();
		
		float deltaX = 0;
		float deltaY = 0;
		
		int width = getWidth();
		int height = getHeight();
		
		if(rectF.top > 0 && isCheckTopAndBottom){
			deltaY = -rectF.top;
		}
		
		if(rectF.bottom < height && isCheckTopAndBottom){
			deltaY = height - rectF.bottom;
		}
		
		if(rectF.left > 0 && isCheckLeftAndRight){
			deltaX = -rectF.left;
		}
		
		if(rectF.right < width && isCheckLeftAndRight){
			deltaX = width - rectF.right;
		}
		
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	private boolean isMoveAction(float dx, float dy) {
		return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
	}

	private class AudoScaleRunnable implements Runnable{
		
		/** target scale value **/
		private float targetScale;
		private float x;
		private float y;

		private final float BIGGER = 1.07F;
		private final float SMALL = 0.93F;
		
		private float tmpScale;
		
		public AudoScaleRunnable(float targetScale, float x, float y) {
			this.targetScale = targetScale;
			this.x = x;
			this.y = y;
			
			if(getScale() < targetScale){
				tmpScale = BIGGER;
			}
			
			if(getScale() > targetScale){
				tmpScale = SMALL;
			}
		}
		
		@Override
		public void run() {
			mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
			
			float currentScale = getScale();
			if((tmpScale > 1.0F && currentScale < targetScale) ||
					(tmpScale < 1.0F && currentScale > targetScale)){
				postDelayed(this, 16);
			}else{
				float scale = targetScale / currentScale;
				mScaleMatrix.postScale(scale, scale, x, y);
				checkBorderAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}
	}
}

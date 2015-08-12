package io.r_a_d.android.widget;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.LinkedHashSet;
import java.util.Set;

//==============================================================================
public class PullDismissLayout
extends      FrameLayout
implements   GestureDetector.OnGestureListener,
             ValueAnimator.AnimatorUpdateListener {
	//--------------------------------------------------------------------------

	private final Set<OnPullDismissListener> LISTENERS = new LinkedHashSet<>();

	private GestureDetectorCompat detector;
	private ValueAnimator animator;

	private int edgeGravity = Gravity.TOP | Gravity.BOTTOM;
	private float pullDist, pullLimit;

	//--------------------------------------------------------------------------

	public PullDismissLayout( Context context ) {
		super( context );
		init( context );
	}

	//--------------------------------------------------------------------------

	public PullDismissLayout( Context context, AttributeSet attrs ) {
		super( context, attrs );
		init( context );
	}

	//--------------------------------------------------------------------------

	public PullDismissLayout( Context context, AttributeSet attrs, int defStyleAttr ) {
		super( context, attrs, defStyleAttr );
		init( context );
	}

	//--------------------------------------------------------------------------

	@TargetApi( Build.VERSION_CODES.LOLLIPOP )
	public PullDismissLayout( Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes ) {
		super( context, attrs, defStyleAttr, defStyleRes );
		init( context );
	}

	//--------------------------------------------------------------------------

	private void init( Context context ) {
		detector = new GestureDetectorCompat( context, this );

		animator = new ValueAnimator();
		animator.addUpdateListener( this );
		animator.setInterpolator( new FastOutSlowInInterpolator() );
		animator.setDuration( 200 );

		pullLimit = 48 * context.getResources().getDisplayMetrics().density;
	}

	//--------------------------------------------------------------------------

	@Override public boolean onTouchEvent( @NonNull MotionEvent ev ) {
		if ( ev.getAction() == MotionEvent.ACTION_UP ) releasePull();
		return detector.onTouchEvent( ev );
	}

	//--------------------------------------------------------------------------

	@Override public boolean onInterceptTouchEvent( MotionEvent ev ) {
		return detector.onTouchEvent( ev ) && ev.getAction() == MotionEvent.ACTION_MOVE;
	}

	//--------------------------------------------------------------------------

	@Override public void onAnimationUpdate( ValueAnimator animation ) {
		pullDist = (Float)animation.getAnimatedValue();
		translateChildren();
	}

	//--------------------------------------------------------------------------

	@Override public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
		return Math.abs( distanceY ) > Math.abs( distanceX ) && pull( -distanceY );
	}

	//--------------------------------------------------------------------------

	@Override public boolean onDown( MotionEvent e ) { return true; }

	@Override public void onLongPress( MotionEvent e ) {}
	@Override public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) { return false; }
	@Override public void onShowPress( MotionEvent e ) {}
	@Override public boolean onSingleTapUp( MotionEvent e ) { return false; }

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	public void setPullEdges( int edgeGravity ) {
		this.edgeGravity = Gravity.VERTICAL_GRAVITY_MASK & edgeGravity;
	}

	//--------------------------------------------------------------------------

	public void addOnPullDismissListener( OnPullDismissListener listener ) {
		LISTENERS.add( listener );
	}

	//--------------------------------------------------------------------------

	public void removeOnPullDismissListener( OnPullDismissListener listener ) {
		LISTENERS.remove( listener );
	}

	//--------------------------------------------------------------------------

	public void resetPull( boolean animate ) {
		if ( animate ) {
			animator.setFloatValues( pullDist, 0 );
			animator.start();

		} else {
			pullDist = 0;
			translateChildren();
		}
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	private boolean pull( float distance ) {
		animator.cancel();
		pullDist += distance;
		if ((( edgeGravity & Gravity.TOP    ) != Gravity.TOP    && pullDist > 0 )
		||   ( edgeGravity & Gravity.BOTTOM ) != Gravity.BOTTOM && pullDist < 0 ) {

			pullDist = 0;
			translateChildren();
			return false;
		}

		translateChildren();
		if ( Math.abs( resistedPull( pullDist )) > pullLimit ) {
			for ( OnPullDismissListener l : LISTENERS ) {
				l.onPullDismissReady( this, pullDist > 0 ? Gravity.TOP : Gravity.BOTTOM );
			}
		}
		return true;
	}

	//--------------------------------------------------------------------------

	private void releasePull() {
		boolean shouldReset = false;
		if ( Math.abs( resistedPull( pullDist )) > pullLimit ) for ( OnPullDismissListener l : LISTENERS ) {
			shouldReset = l.onPullDismiss( this, pullDist > 0 ? Gravity.TOP : Gravity.BOTTOM ) || shouldReset;

		} else resetPull( true );
		if ( shouldReset ) resetPull( true );
	}

	//--------------------------------------------------------------------------

	private void translateChildren() {
		final float resistedPull = resistedPull( pullDist );
		for ( int i = 0; i < getChildCount(); i++ ) {
			getChildAt( i ).setTranslationY( resistedPull );
		}
	}

	//--------------------------------------------------------------------------

	// https://en.wikipedia.org/wiki/Logistic_function
	private float resistedPull( float distance ) {
		final double L = getHeight() / 4;
		final double k = 1d / L;
		final double x = distance;

		return (float)(( L / ( 1 + Math.exp( -k * x ))) - ( L / 2 ));
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	public interface OnPullDismissListener {
		//----------------------------------------------------------------------

		void onPullDismissReady( PullDismissLayout layout, int edgeGravity );

		//----------------------------------------------------------------------

		boolean onPullDismiss( PullDismissLayout layout, int edgeGravity );

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

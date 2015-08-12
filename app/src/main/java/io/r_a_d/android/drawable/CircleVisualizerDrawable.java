package io.r_a_d.android.drawable;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.audiofx.Visualizer;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.animation.AccelerateInterpolator;

//==============================================================================
public class CircleVisualizerDrawable
extends      Drawable
implements   AnimatorUpdateListener {
	//--------------------------------------------------------------------------

	private ValueAnimator
		upAnimator = new ValueAnimator(),
		downAnimator = new ValueAnimator();

	private Paint paint;
	private float amplitude, maxRadius, minRadius, centerX, centerY;

	//--------------------------------------------------------------------------

	public CircleVisualizerDrawable() {
		final long upDuration = (long)( 1000f / ( Visualizer.getMaxCaptureRate() / 1000f ));

		upAnimator.setInterpolator( new LinearOutSlowInInterpolator() );
		upAnimator.setDuration( upDuration * 2 );
		upAnimator.addUpdateListener( this );

		downAnimator.setInterpolator( new AccelerateInterpolator() );
		downAnimator.setStartDelay( upDuration * 2 );
		downAnimator.addUpdateListener( this );


		paint = new Paint();
		paint.setAntiAlias( true );
		paint.setStyle( Style.FILL );
		setColor( 0xffffff );
	}

	//--------------------------------------------------------------------------

	@Override public void onBoundsChange( Rect bounds ) {
		minRadius = 0;
		maxRadius = Math.min( getBounds().exactCenterX(), getBounds().exactCenterY() );
		centerX = bounds.exactCenterX();
		centerY = bounds.exactCenterY();
	}

	//--------------------------------------------------------------------------

	@Override public void draw( Canvas canvas ) {
		canvas.drawCircle(
			centerX,
			centerY,
			minRadius + (( maxRadius - minRadius ) * amplitude ),
			paint );
	}

	//--------------------------------------------------------------------------

	@Override public void setAlpha( int alpha ) {}

	//--------------------------------------------------------------------------

	@Override public void setColorFilter( ColorFilter cf ) {}

	//--------------------------------------------------------------------------

	@Override public int getOpacity() { return 0xff; }

	//--------------------------------------------------------------------------

	@Override public void onAnimationUpdate( ValueAnimator animation ) {
		amplitude = (float)animation.getAnimatedValue();
		invalidateSelf();
	}

	//--------------------------------------------------------------------------

	public void setColor( int color ) {
		paint.setColor( color );
		paint.setAlpha( 0x19 ); // 10%
	}

	//--------------------------------------------------------------------------

	public void setMinRadius( float minRadius ) {
		this.minRadius = minRadius;
		invalidateSelf();
	}

	//--------------------------------------------------------------------------

	public void setMaxRadius( float maxRadius ) {
		this.maxRadius = maxRadius;
		invalidateSelf();
	}

	//--------------------------------------------------------------------------

	public void setCenter( float centerX, float centerY ) {
		this.centerX = centerX;
		this.centerY = centerY;
		invalidateSelf();
	}

	//--------------------------------------------------------------------------

	// Doesn't actually set the visible amplitude directly, just activates the
	// animators if it's greater than the current amplitude
	public void setAmplitude( float amp ) {
		if ( amp > amplitude ) {
			upAnimator.cancel();
			downAnimator.cancel();

			upAnimator.setFloatValues( amplitude, amp );
			downAnimator.setFloatValues( amp, 0 );
			downAnimator.setDuration( (long)( amp * 1000 ));

			upAnimator.start();
			downAnimator.start();
		}
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

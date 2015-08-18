package io.r_a_d.android.drawable;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;

import prime.lib.android.radioplayer.VisualizedAudioTrackRenderer;

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

		upAnimator.setInterpolator( new LinearOutSlowInInterpolator() );
		upAnimator.setDuration( VisualizedAudioTrackRenderer.rateToInterval( VisualizedAudioTrackRenderer.getPreferredCaptureRate() ));
		upAnimator.addUpdateListener( this );

		downAnimator.setInterpolator( upAnimator.getInterpolator() );
		downAnimator.setStartDelay( upAnimator.getDuration() );
		downAnimator.setDuration( upAnimator.getDuration() * 4 );
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

	public void setAmplitude( float amp ) {
		if ( amp > amplitude ) {
			upAnimator.cancel();
			upAnimator.setFloatValues( amplitude, amp );
			upAnimator.start();

			downAnimator.cancel();
			downAnimator.setFloatValues( amp, 0 );
			downAnimator.start();
		}
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

package io.r_a_d.android.drawable;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//==============================================================================
public class PlayPauseDrawable
extends      Drawable
implements   ValueAnimator.AnimatorUpdateListener {
	//--------------------------------------------------------------------------

	@IntDef({STATE_PLAY,STATE_PAUSE})
	@Retention( RetentionPolicy.SOURCE )
	@interface ButtonState {}

	public static final int STATE_PLAY = 0, STATE_PAUSE = 1;

	private static final int[][][] POINTS = new int[][][] {
		{ // Play
			{ 16, 10,  38, 24,  38, 24,  16, 24, }, // Top
			{ 16, 24,  38, 24,  38, 24,  16, 38, }, // Botom
		},
		{ // Between
			{ 13, 11,  38, 18,  38, 24,  13, 24, }, // Top
			{ 13, 24,  38, 24,  38, 30,  13, 37, }, // Bottom
		},
		{ // Pause
			{ 10, 12,  38, 12,  38, 20,  10, 20, }, // Top
			{ 10, 28,  38, 28,  38, 36,  10, 36, }, // Bottom
		},
	};
	private static final RectF pointBounds = new RectF( 0, 0, 48, 48 );

	private final Path path = new Path(), drawPath = new Path();
	private final Paint paint = new Paint();
	private final Matrix matrix = new Matrix();
	private final ValueAnimator stateAnimator = new ValueAnimator();
	private final ValueAnimator alphaAnimator = new ValueAnimator();

	private boolean invert = false;

	@ButtonState private int state;

	private float interpolation;

	//--------------------------------------------------------------------------

	public PlayPauseDrawable( int color ) {
		setInterpolation( 0 );
		paint.setColor( color );
		paint.setStyle( Paint.Style.FILL );
		paint.setAntiAlias( true );

		stateAnimator.setInterpolator( new FastOutSlowInInterpolator() );
		stateAnimator.setDuration( 300 );
		stateAnimator.addUpdateListener( this );

		alphaAnimator.setInterpolator( new FastOutSlowInInterpolator() );
		alphaAnimator.setDuration( 300 );
		alphaAnimator.addUpdateListener( this );
	}

	//--------------------------------------------------------------------------

	private void setInterpolation( float interpolation ) {
		this.interpolation = Math.min( Math.max( interpolation, 0 ), 1 ); // Clamp

		float
			expandedInterpolation = this.interpolation * ( POINTS.length - 1 ),
			subInterpolation = expandedInterpolation % 1;
		int indexA = (int)expandedInterpolation, indexB = Math.min( indexA + 1, POINTS.length - 1 );

		path.reset();

		float x, y;
		for ( int i = 0; i < POINTS[indexA].length; i++ ) {
			for ( int j = 0; j < POINTS[indexA][i].length; j += 2 ) {

				x = interpolate( POINTS[indexA][i][j],   POINTS[indexB][i][j],   subInterpolation );
				y = interpolate( POINTS[indexA][i][j+1], POINTS[indexB][i][j+1], subInterpolation );

				if ( j == 0 ) path.moveTo( x, y );
				else          path.lineTo( x, y );
			}
			path.close();
		}

		if ( this.interpolation == 0 ) invert = false;
		if ( this.interpolation == 1 ) invert = true;

		invalidateSelf();
	}

	//--------------------------------------------------------------------------

	private float interpolate( float a, float b, float i ) {
		return a + (( b - a ) * i );
	}

	//--------------------------------------------------------------------------

	@Override public void draw( Canvas c ) {
		matrix.reset();
		float scale = Math.min( getBounds().width() / 48f, getBounds().height() / 48f );

		float
			cx = getBounds().exactCenterX(),
			cy = getBounds().exactCenterY();

		matrix.postTranslate( cx - 24, cy - 24 );
		matrix.postScale( scale, scale, cx, cy );
		matrix.postRotate( 90 * interpolation, cx, cy );
		if ( invert ) matrix.postScale( 1, -1, cx, cy );

		drawPath.reset();
		drawPath.addPath( path, matrix );

		c.drawPath( drawPath, paint );
	}

	//--------------------------------------------------------------------------

	@Override public void setAlpha( @IntRange( from = 0, to = 255 ) int a ) {
		alphaAnimator.cancel();
		alphaAnimator.setIntValues( paint.getAlpha(), a );
		alphaAnimator.start();
	}

	//--------------------------------------------------------------------------

	@Override public void setColorFilter( ColorFilter colorFilter ) {}

	//--------------------------------------------------------------------------

	@Override public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	//--------------------------------------------------------------------------

	@Override public void onAnimationUpdate( ValueAnimator animation ) {
		if ( animation == stateAnimator ) {
			setInterpolation( (Float)animation.getAnimatedValue() );
		}

		if ( animation == alphaAnimator ) {
			paint.setAlpha( (Integer)animation.getAnimatedValue() );
			invalidateSelf();
		}
	}

	//--------------------------------------------------------------------------

	public void set( @ButtonState int state ) {
		this.state = state;
		setInterpolation( state == STATE_PLAY ? 0f : 1f );
	}

	//--------------------------------------------------------------------------

	public void animateTo( @ButtonState int state ) {
		if ( this.state != state ) animateToggle();
	}

	//--------------------------------------------------------------------------

	public void animateToggle() {
		state = state == STATE_PLAY ? STATE_PAUSE : STATE_PLAY;

		stateAnimator.setFloatValues( interpolation, state == STATE_PLAY ? 0f : 1f );
		stateAnimator.start();
	}

	//--------------------------------------------------------------------------

	public void endAnimation() {
		if ( stateAnimator.getValues() != null ) stateAnimator.end();
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

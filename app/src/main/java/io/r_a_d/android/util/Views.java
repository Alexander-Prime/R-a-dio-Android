package io.r_a_d.android.util;

import android.view.View;

//==============================================================================
public abstract class Views {
	//--------------------------------------------------------------------------

	public static float getChildX( View parent, View child ) {
		if ( child == parent ) return 0;
		else return child.getX() + getChildX( parent, (View)child.getParent() );
	}

	//--------------------------------------------------------------------------

	public static float getChildY( View parent, View child ) {
		if ( child == parent ) return 0;
		else return child.getY() + getChildY( parent, (View)child.getParent() );
	}

	//--------------------------------------------------------------------------

	public static float getChildCenterX( View parent, View child ) {
		return getChildX( parent, child ) + ( child.getWidth() / 2f );
	}

	//--------------------------------------------------------------------------

	public static float getChildCenterY( View parent, View child ) {
		return getChildY( parent, child ) + ( child.getHeight() / 2f );
	}

	//--------------------------------------------------------------------------

	public static float getDiagonal( View view ) {
		return (float)Math.sqrt(
			view.getWidth()  * view.getWidth() +
			view.getHeight() * view.getHeight() );
	}

	//--------------------------------------------------------------------------

	public static float getSmallestLength( View view ) {
		return Math.min( view.getWidth(), view.getHeight() );
	}

	//--------------------------------------------------------------------------

	public static float getLargestLength( View view ) {
		return Math.max( view.getWidth(), view.getHeight() );
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

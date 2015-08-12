package io.r_a_d.model;

//==============================================================================
public class Song {
	//--------------------------------------------------------------------------

	private static final int INTERVAL = 30;

	public static final int
		TYPE_RANDOM = 0,
		TYPE_REQUEST = 1;

	//--------------------------------------------------------------------------

	public int type = TYPE_RANDOM;
	public int length = Integer.MAX_VALUE;

	public long timestamp = System.currentTimeMillis() / 1000;

	public String meta;

	//--------------------------------------------------------------------------

	public int getElapsedSeconds() {
		return (int)(( System.currentTimeMillis() / 1000 ) - timestamp );
	}

	//--------------------------------------------------------------------------

	public boolean isFinishedInterval() {
		final int now = (int)( System.currentTimeMillis() / 1000 );
		final int tempLength = length == Integer.MAX_VALUE ? 0 : length;
		final int secondsAfter = (int)( now - ( timestamp + tempLength ));

		return secondsAfter >= 0 && secondsAfter % INTERVAL == 0;
	}

	//--------------------------------------------------------------------------

	public boolean isFinished() {
		final int now = (int)( System.currentTimeMillis() / 1000 );
		final int tempLength = length == Integer.MAX_VALUE ? 0 : length;

		return now >= timestamp + tempLength;
	}

	//--------------------------------------------------------------------------

	public boolean isRequested() { return type == TYPE_REQUEST; }

	//--------------------------------------------------------------------------

	public boolean hasLength() { return length < Integer.MAX_VALUE && length > 0; }

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

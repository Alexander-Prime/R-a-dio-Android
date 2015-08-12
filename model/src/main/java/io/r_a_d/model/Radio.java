package io.r_a_d.model;

import java.util.List;

//==============================================================================
public class Radio {
	//--------------------------------------------------------------------------

	public String
		np,
		thread;

	public int
		listeners,
		trackid;

	public int // Really boolean, why are these numeric?
		isafkstream,
		requesting;

	public long
		start_time,
		end_time,
		current;

	public DJ
		dj;

	public List<Song>
		queue;

	public List<Song>
		lp;

	//--------------------------------------------------------------------------

	public Song nowPlaying() {
		Song nowPlaying = new Song();

		nowPlaying.length = (int)( end_time - start_time );
		nowPlaying.meta = np;
		nowPlaying.timestamp = start_time;

		return nowPlaying;
	}

	//--------------------------------------------------------------------------

	public boolean isAfk() { return isafkstream != 0; }

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

package prime.lib.android.radioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

//==============================================================================
public class RadioPlayer
implements   ExoPlayer.Listener,
             AudioManager.OnAudioFocusChangeListener,
             VisualizedAudioTrackRenderer.VisualizerListener {
	//--------------------------------------------------------------------------

	@IntDef({ PLAYBACK_STOPPED, PLAYBACK_BUFFERING, PLAYBACK_PLAYING, PLAYBACK_PAUSED })
	@Retention( RetentionPolicy.SOURCE )
	public @interface PlaybackState{};
	public static final int
		PLAYBACK_STOPPED   = 0,
		PLAYBACK_BUFFERING = 1,
		PLAYBACK_PLAYING   = 2,
		PLAYBACK_PAUSED    = 3;


	private final List<Listener> LISTENERS = new ArrayList<>();
	private final Context CONTEXT;

	private final Runnable EXEC_STOP = new Runnable() {
		@Override public void run() { stop(); }
	};


	private ExoPlayer player;
	private VisualizedAudioTrackRenderer renderer;
	private Handler handler;

	private float volume = 1f;

	//--------------------------------------------------------------------------

	public RadioPlayer( Context context ) {
		player = ExoPlayer.Factory.newInstance( 1 );
		player.addListener( this );

		CONTEXT = context.getApplicationContext();

		handler = new Handler( context.getMainLooper() );
	}

	//--------------------------------------------------------------------------

	@Override public void onPlayerStateChanged( boolean playWhenReady, int newState ) {
		if ( newState == ExoPlayer.STATE_ENDED ) player.stop();

		notifyOnPlaybackStateChanged();
	}

	//--------------------------------------------------------------------------

	@Override public void onPlayWhenReadyCommitted() {
	}

	//--------------------------------------------------------------------------

	@Override public void onPlayerError( ExoPlaybackException e ) {
		e.printStackTrace();
	}

	//--------------------------------------------------------------------------

	@Override public void onAudioFocusChange( int focus ) {
		switch ( focus ) {
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				break;

			case AudioManager.AUDIOFOCUS_GAIN:
				break;
		}
	}

	//--------------------------------------------------------------------------

	@Override public void onVisualizerCapture( Visualizer visualizer, double rms ) {
		for ( Listener l : LISTENERS ) l.onVisualizerCapture( visualizer, rms );
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	public void playPause() {
		final @PlaybackState int oldState = getPlaybackState();
		switch ( oldState ) {
			default:
			case PLAYBACK_STOPPED:
			case PLAYBACK_PAUSED:
				play();
				break;

			case PLAYBACK_BUFFERING:
			case PLAYBACK_PLAYING:
				deferStop();
				break;
		}
	}

	//--------------------------------------------------------------------------

	public void addListener( Listener listener ) {
		LISTENERS.add( listener );
	}

	//--------------------------------------------------------------------------

	public void removeListener( Listener listener ) {
		LISTENERS.remove( listener );
	}

	//--------------------------------------------------------------------------

	public boolean isPlaying() {
		return getPlaybackState() == PLAYBACK_PLAYING;
	}

	//--------------------------------------------------------------------------

	public boolean isStreaming() {
		return
			getPlaybackState() == PLAYBACK_PLAYING ||
			getPlaybackState() == PLAYBACK_BUFFERING;
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	private TrackRenderer initRenderer() {
		DataSource dataSource = new DefaultUriDataSource( CONTEXT, CONTEXT.getResources().getString( R.string.userAgent ));
		SampleSource sampleSource = new ExtractorSampleSource(
			Uri.parse( "https://stream.r-a-d.io/main.mp3" ),
			dataSource,
			new Mp3Extractor(),
			new DefaultAllocator( 64 * 1024 ),
			64 * 1024 * 4
		);

		renderer = new VisualizedAudioTrackRenderer( sampleSource );
		renderer.addListener( this );
		return renderer;
	}

	//--------------------------------------------------------------------------

	@PlaybackState public int getPlaybackState() {
		switch ( player.getPlaybackState() ) {
			default:
			case ExoPlayer.STATE_IDLE:
			case ExoPlayer.STATE_ENDED:
				return PLAYBACK_STOPPED;

			case ExoPlayer.STATE_PREPARING:
			case ExoPlayer.STATE_BUFFERING:
				return PLAYBACK_BUFFERING;

			case ExoPlayer.STATE_READY:
				if ( !player.getPlayWhenReady() ) return PLAYBACK_PAUSED;
				return volume > 0 ? PLAYBACK_PLAYING : PLAYBACK_PAUSED;
		}
	}

	//--------------------------------------------------------------------------

	private void notifyOnPlaybackStateChanged() {
		final int state = getPlaybackState();
		renderer.setVisualizerEnabled( state == PLAYBACK_PLAYING );
		for ( Listener l : LISTENERS ) l.onPlaybackStateChanged( state );
	}

	//--------------------------------------------------------------------------

	private void setVolume( @FloatRange( from=0f, to=1f ) float newVolume ) {
		volume = newVolume;
		player.sendMessage( renderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, newVolume );
	}

	//--------------------------------------------------------------------------

	public void play() {
		// Prevent deferred stopping after we play again
		handler.removeCallbacks( EXEC_STOP );

		switch ( player.getPlaybackState() ) {
			case ExoPlayer.STATE_ENDED:
				player.stop();
				// Now idle, keep going

			case ExoPlayer.STATE_IDLE:
				player.prepare( initRenderer() );
				player.setPlayWhenReady( true );
				break;
		}

		setVolume( 1f );
		if ( player.getPlaybackState() == ExoPlayer.STATE_READY ) notifyOnPlaybackStateChanged();
	}

	//--------------------------------------------------------------------------

	public void deferStop() {
		setVolume( 0f );
		handler.postDelayed( EXEC_STOP, 30 * 1000 );
		if ( player.getPlaybackState() == ExoPlayer.STATE_READY ) notifyOnPlaybackStateChanged();
	}

	//--------------------------------------------------------------------------

	public void stop() {
		// Don't need to stop again if we are doing it now
		handler.removeCallbacks( EXEC_STOP );
		player.stop();
	}

	//--------------------------------------------------------------------------

	public void release() {
		player.release();
		renderer.removeListener( this );
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	public interface Listener {
		//----------------------------------------------------------------------

		void onPlaybackStateChanged( @PlaybackState int newState );

		//----------------------------------------------------------------------

		void onVisualizerCapture( Visualizer visualizer, double rms );

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

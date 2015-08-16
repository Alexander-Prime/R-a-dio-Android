package prime.lib.android.radioplayer;

import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.support.annotation.FloatRange;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.drm.DrmSessionManager;

import java.util.LinkedHashSet;
import java.util.Set;

//==============================================================================
public class VisualizedAudioTrackRenderer
extends      MediaCodecAudioTrackRenderer
implements   Visualizer.OnDataCaptureListener {
	//--------------------------------------------------------------------------

	private final Set<VisualizerListener> LISTENERS = new LinkedHashSet<>();

	private Visualizer visualizer;
	private Equalizer equalizer;

	private int[] formattedWaveformData;
	private boolean visEnabled;

	//--------------------------------------------------------------------------

	// Prefer refreshing 6 times a second
	public static int getPreferredCaptureRate() {
		return Math.min( Visualizer.getMaxCaptureRate(), 6000 );
	}

	//--------------------------------------------------------------------------

	public static int rateToInterval( int milliHz ) {
		return (int)( 1000f / ( milliHz / 1000f ));
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	public VisualizedAudioTrackRenderer( SampleSource source ) {
		super( source );
	}

	//--------------------------------------------------------------------------

	public VisualizedAudioTrackRenderer( SampleSource source, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys ) {
		super( source, drmSessionManager, playClearSamplesWithoutKeys );
	}

	//--------------------------------------------------------------------------

	public VisualizedAudioTrackRenderer( SampleSource source, Handler eventHandler, EventListener eventListener ) {
		super( source, eventHandler, eventListener );
	}

	//--------------------------------------------------------------------------

	public VisualizedAudioTrackRenderer( SampleSource source, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, EventListener eventListener ) {
		super( source, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener );
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	// Super is a no-op as of the time of writing, but we call it anyway to
	// avoid confusion if it starts doing something later
	@Override protected void onAudioSessionId( int id ) {
		super.onAudioSessionId( id );

		formattedWaveformData = new int[Visualizer.getCaptureSizeRange()[1]];

		visualizer = new Visualizer( id );
		visualizer.setCaptureSize( Visualizer.getCaptureSizeRange()[1] );
		visualizer.setDataCaptureListener( this, getPreferredCaptureRate(), true, false );
		setVisualizerEnabled( visEnabled );

		// This is needed to prevent system volume affecting the sample amplitude
		equalizer = new Equalizer( 0, id );
	}

	//--------------------------------------------------------------------------

	@Override protected void onDisabled() {
		super.onDisabled();

		equalizer.release();
		visualizer.release();
	}

	//--------------------------------------------------------------------------

	@Override public void onWaveFormDataCapture( Visualizer visualizer, byte[] waveform, int samplingRate ) {
		// If we do this right after creating it, we get a short burst of
		// full-volume music when playback starts
		equalizer.setEnabled( true );


		convertWaveformData( waveform );
		for ( VisualizerListener l : LISTENERS ) l.onVisualizerCapture( visualizer, rmsFraction() );
	}

	//--------------------------------------------------------------------------

	@Override public void onFftDataCapture( Visualizer visualizer, byte[] fft, int samplingRate ) {}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	void addListener( VisualizerListener listener ) {
		LISTENERS.add( listener );
	}

	//--------------------------------------------------------------------------

	void removeListener( VisualizerListener listener ) {
		LISTENERS.remove( listener );
	}

	//--------------------------------------------------------------------------

	void setVisualizerEnabled( boolean enabled ) {
		visEnabled = enabled;

		// IllegalStateException is thrown if setEnabled() is called after
		// the player stops, I think?? Probably harmless, ignore for now.
		try { visualizer.setEnabled( enabled ); }
		catch ( NullPointerException | IllegalStateException ignored ) {}
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	private void convertWaveformData( byte[] waveformData ) {
		final int length = waveformData.length;

		// convert from unsigned 8 bit to signed 16 bit
		for ( int i = 0; i < length; i++ ) {
			formattedWaveformData[i] = ( (int) waveformData[i] & 0xFF ) - 128;
		}
	}

	//--------------------------------------------------------------------------

	private double rmsValue() {
		if ( formattedWaveformData.length == 0 ) return 0;

		final int length = formattedWaveformData.length;
		double rms = 0;
		for ( int i : formattedWaveformData ) rms += i * i;
		return Math.sqrt( rms / length );
	}

	//--------------------------------------------------------------------------

	private @FloatRange( from = 0, to = 1 ) float rmsFraction() {
		return (float)Math.min( Math.max( rmsValue() / 60, 0 ), 1 );
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	interface VisualizerListener {
		//----------------------------------------------------------------------

		void onVisualizerCapture( Visualizer visualizer, double rms );

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

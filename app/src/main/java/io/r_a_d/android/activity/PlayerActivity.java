package io.r_a_d.android.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.r_a_d.android.R;
import io.r_a_d.android.drawable.CircleVisualizerDrawable;
import io.r_a_d.android.drawable.PlayPauseDrawable;
import io.r_a_d.android.service.RadioInfoService;
import io.r_a_d.android.service.RadioPlaybackService;
import io.r_a_d.android.util.Views;
import io.r_a_d.android.widget.PullDismissLayout;
import io.r_a_d.model.Radio;
import io.r_a_d.model.Song;
import prime.lib.android.radioplayer.RadioPlayer;

//==============================================================================
public class PlayerActivity
extends      AppCompatActivity
implements   RadioInfoService.Listener,
             RadioPlaybackService.Listener,
             PullDismissLayout.OnPullDismissListener {
	//--------------------------------------------------------------------------

	private ServiceConnection
		radioInfoService_connection,
		radioPlaybackService_connection;

	private RadioInfoService.Binder radioInfo_binder;
	private RadioPlaybackService.Binder radioPlayback_binder;

	private PlayPauseDrawable playPause_drawable;
	private CircleVisualizerDrawable visualizer_drawable;

	@Bind( R.id.pullDismiss ) PullDismissLayout pullDismiss;

	@Bind( R.id.lastPlayed_title ) TextView lastPlayed_title;
	@Bind( R.id.upNext_title     ) TextView upNext_title;

	@Bind( R.id.upNext ) View upNext;
	@Bind( R.id.thread ) View thread;

	@Bind( R.id.nowPlaying_card ) View nowPlaying_card;
	@Bind( R.id.nowPlaying      ) View nowPlaying;

	@Bind( R.id.avatar    ) ImageView   avatar;
	@Bind( R.id.name      ) TextView    name;
	@Bind( R.id.listeners ) TextView    listeners;
	@Bind( R.id.playPause ) ImageButton playPause;
	@Bind( R.id.title     ) TextView    title;
	@Bind( R.id.progress  ) ProgressBar progress;
	@Bind( R.id.elapsed   ) TextView    elapsed;
	@Bind( R.id.duration  ) TextView    duration;

	//--------------------------------------------------------------------------

	@Override protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );


		setVolumeControlStream( AudioManager.STREAM_MUSIC );


		setContentView( R.layout.activity_player );
		ButterKnife.bind( this );

		pullDismiss.addOnPullDismissListener( this );

		playPause_drawable = new PlayPauseDrawable( getResources().getColor( R.color.white_a100 ));
		playPause.setImageDrawable( playPause_drawable );

		visualizer_drawable = new CircleVisualizerDrawable();
		nowPlaying.setBackgroundDrawable( visualizer_drawable );
		nowPlaying.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override public void onGlobalLayout() {
				visualizer_drawable.setMaxRadius( Views.getLargestLength( nowPlaying ) / 2 );
				visualizer_drawable.setCenter(
					Views.getChildCenterX( nowPlaying, playPause ),
					Views.getChildCenterY( nowPlaying, playPause ));
			}
		} );

		title.setSelected( true );


		radioInfoService_connection = new ServiceConnection() {
			@Override public void onServiceConnected( ComponentName name, IBinder service ) {
				radioInfo_binder = (RadioInfoService.Binder)service;
				radioInfo_binder.addListener( PlayerActivity.this );
				if ( radioInfo_binder.getRadio() != null ) {
					onRadioUpdated( radioInfo_binder.getRadio() );
					onTimeUpdated( radioInfo_binder.getRadio() );
				}
			}

			@Override public void onServiceDisconnected( ComponentName name ) {
				radioInfo_binder.removeListener( PlayerActivity.this );
			}
		};

		radioPlaybackService_connection = new ServiceConnection() {
			@Override public void onServiceConnected( ComponentName name, IBinder service ) {
				radioPlayback_binder = (RadioPlaybackService.Binder)service;
				radioPlayback_binder.addListener( PlayerActivity.this );
				onPlaybackStateChanged( radioPlayback_binder.getPlaybackState() );
				playPause_drawable.endAnimation();
			}

			@Override public void onServiceDisconnected( ComponentName name ) {
				radioPlayback_binder.removeListener( PlayerActivity.this );
			}
		};
	}

	//--------------------------------------------------------------------------

	@Override protected void onStart() {
		super.onStart();

		bindService( new Intent( this, RadioInfoService    .class ), radioInfoService_connection,     BIND_AUTO_CREATE );
		bindService( new Intent( this, RadioPlaybackService.class ), radioPlaybackService_connection, BIND_AUTO_CREATE );
	}

	//--------------------------------------------------------------------------

	@Override protected void onStop() {
		super.onStop();

		unbindService( radioInfoService_connection );
		unbindService( radioPlaybackService_connection );
		pullDismiss.resetPull( false );
	}

	//--------------------------------------------------------------------------

	@Override public void onRadioUpdated( Radio radio ) {
		lastPlayed_title.setText( radio.lp.get( 0 ).meta );
		upNext_title.setText( radio.queue.get( 0 ).meta );

		Picasso.with( this )
			.load( radio.dj.getAvatarUrl() )
			.placeholder( R.drawable.logo )
			.into( avatar );
		name.setText( radio.dj.djname );
		listeners.setText( getResources().getString( R.string.format_listeners, radio.listeners ));

		final Song s = radio.nowPlaying();
		title.setText( s.meta );
		setTimeValues( s.getElapsedSeconds(), s.length );

		visualizer_drawable.setColor( radio.dj.getColor() );


		if ( radio.isAfk() ) {
			upNext.setVisibility( View.VISIBLE );
			thread.setVisibility( View.GONE );
			pullDismiss.setPullEdges( Gravity.TOP | Gravity.BOTTOM );

		} else {
			upNext.setVisibility( View.GONE );
			thread.setVisibility( View.VISIBLE );
			pullDismiss.setPullEdges( Gravity.TOP );
		}
	}

	//--------------------------------------------------------------------------

	@Override public void onTimeUpdated( Radio radio ) {
		final Song s = radio.nowPlaying();
		setTimeValues( s.getElapsedSeconds(), s.length );
	}

	//--------------------------------------------------------------------------

	@Override public void onPlaybackStateChanged( @RadioPlayer.PlaybackState int newState ) {
		switch ( newState ) {
			default:
			case RadioPlayer.PLAYBACK_STOPPED:
			case RadioPlayer.PLAYBACK_PAUSED:
				playPause_drawable.animateTo( PlayPauseDrawable.STATE_PLAY );
				break;

			case RadioPlayer.PLAYBACK_PLAYING:
			case RadioPlayer.PLAYBACK_BUFFERING:
				playPause_drawable.animateTo( PlayPauseDrawable.STATE_PAUSE );
				break;
		}

		if ( newState == RadioPlayer.PLAYBACK_BUFFERING ) {
			playPause_drawable.setAlpha( (int)( 0xff * 0.3 ));
			playPause.setEnabled( false );
		} else {
			playPause_drawable.setAlpha( 0xff );
			playPause.setEnabled( true );
		}
	}

	//--------------------------------------------------------------------------

	@Override public void onVisualizerCapture( Visualizer visualizer, double rms ) {
		visualizer_drawable.setAmplitude( (float)rms );
	}

	//--------------------------------------------------------------------------

	@Override public void onPullDismissReady( PullDismissLayout layout, int edgeGravity ) {}

	//--------------------------------------------------------------------------

	@Override public boolean onPullDismiss( PullDismissLayout layout, int edgeGravity ) {
		switch ( edgeGravity & Gravity.VERTICAL_GRAVITY_MASK ) {

			case Gravity.TOP:
				onLastPlayedClick();
				break;

			case Gravity.BOTTOM:
				onUpNextClick();
				break;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	@OnClick( R.id.playPause ) void onPlayPauseClick() {
		radioPlayback_binder.playPause();
	}

	//--------------------------------------------------------------------------

	@OnClick( R.id.share ) void onShareClick() {
		try {
			Intent n = new Intent();
			n.setAction( Intent.ACTION_SEND );
			n.putExtra( Intent.EXTRA_TEXT, radioInfo_binder.getStreamUri().toString() );
			n.setType( "text/plain" );
			startActivity( Intent.createChooser( n, getResources().getString( R.string.share_title )));

		} catch ( NullPointerException e ) {
			Toast.makeText( this, getResources().getString( R.string.notice_noStreamInfo ), Toast.LENGTH_SHORT ).show();
		}
	}

	//--------------------------------------------------------------------------

	@OnClick( R.id.lastPlayed ) void onLastPlayedClick() {
		startActivity(
			new Intent( this, LastPlayedActivity.class ),
			ActivityOptionsCompat.makeSceneTransitionAnimation(
				this,
				Pair.create( nowPlaying_card, "nowPlaying_card" ),
				Pair.create( (View)lastPlayed_title, "lastPlayed_title" )
			).toBundle() );
	}

	//--------------------------------------------------------------------------

	@OnClick( R.id.upNext ) void onUpNextClick() {
		startActivity(
			new Intent( this, UpNextActivity.class ),
			ActivityOptionsCompat.makeSceneTransitionAnimation(
				this,
				Pair.create( nowPlaying_card, "nowPlaying_card" ),
				Pair.create( (View)upNext_title, "upNext_title" )
			).toBundle() );
	}

	//--------------------------------------------------------------------------

	@OnClick( R.id.thread ) void onThreadClick() {
		startActivity( new Intent(
			Intent.ACTION_VIEW,
			Uri.parse( radioInfo_binder.getRadio().thread )));
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	private void setTimeValues( int elapsedSeconds, int durationSeconds ) {
		progress.setMax( durationSeconds );
		progress.setProgress( elapsedSeconds  );

		elapsed .setText( formatElapsedTime( elapsedSeconds  ));
		duration.setText( formatElapsedTime( durationSeconds ));
	}

	//--------------------------------------------------------------------------

	private String formatElapsedTime( long elapsedSeconds ) {
		final Resources res = getResources();

	    // Break the elapsed seconds into hours, minutes, and seconds.
        final long hours   =   elapsedSeconds / 3600;
	    final long minutes = ( elapsedSeconds % 3600 ) / 60;
		final long seconds =   elapsedSeconds          % 60;

        // Format the broken-down time.
	    if ( hours > 0 ) return res.getString( R.string.format_durationHMS, hours, minutes, seconds );
        else             return res.getString( R.string.format_durationMS,         minutes, seconds );
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------
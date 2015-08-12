package io.r_a_d.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import io.r_a_d.android.R;
import io.r_a_d.android.activity.PlayerActivity;
import io.r_a_d.model.Radio;
import prime.lib.android.radioplayer.RadioPlayer;
import prime.lib.android.radioplayer.RadioPlayer.PlaybackState;

import static prime.lib.android.radioplayer.RadioPlayer.PLAYBACK_BUFFERING;
import static prime.lib.android.radioplayer.RadioPlayer.PLAYBACK_PAUSED;
import static prime.lib.android.radioplayer.RadioPlayer.PLAYBACK_PLAYING;
import static prime.lib.android.radioplayer.RadioPlayer.PLAYBACK_STOPPED;

//==============================================================================
public class RadioPlaybackService
extends      Service
implements   ServiceConnection,
             RadioInfoService.Listener,
             RadioPlayer.Listener,
             Target {
	//--------------------------------------------------------------------------

	public static final int NOTIFICATION_ID = 1;
	public static final String ACTION_PLAYPAUSE = "io.r_a_d.android.action.PLAYPAUSE";
	public static final String ACTION_STOP      = "io.r_a_d.android.action.STOP";

	private final List<Listener> LISTENERS = new ArrayList<>();

	private PendingIntent content_intent;
	private PendingIntent playPause_intent;
	private PendingIntent stop_intent;
	private BroadcastReceiver playPause_receiver = new BroadcastReceiver() {
		@Override public void onReceive( Context context, Intent intent ) {
			player.playPause();
		}
	};
	private BroadcastReceiver stop_receiver = new BroadcastReceiver() {
		@Override public void onReceive( Context context, Intent intent ) {
			stopSelf();
		}
	};

	private MediaSessionCompat session;
	private RadioInfoService.Binder radioInfo_binder;
	private RadioPlayer player;
	private Bitmap djAvatar;
	private Handler handler;
	private boolean isBound;

	//--------------------------------------------------------------------------

	@Override public void onCreate() {
		super.onCreate();

		bindService( new Intent( this, RadioInfoService.class ), this, BIND_AUTO_CREATE );

		player = new RadioPlayer( this );
		player.addListener( this );

		content_intent = PendingIntent.getActivity(
			this, 0,
			new Intent( this, PlayerActivity.class ), 0 );

		playPause_intent = PendingIntent.getBroadcast(
			this, 0,
			new Intent( ACTION_PLAYPAUSE ), 0 );

		stop_intent = PendingIntent.getBroadcast(
			this, 0,
			new Intent( ACTION_STOP ), 0 );

		registerReceiver( playPause_receiver, new IntentFilter( ACTION_PLAYPAUSE ));
		registerReceiver( stop_receiver,      new IntentFilter( ACTION_STOP      ));


		final PendingIntent mediaSessionIntent = PendingIntent.getService(
			this, 0,
			new Intent( this, getClass() ), 0 );

		session = new MediaSessionCompat( this, getResources().getString( R.string.app_name ), new ComponentName( this, getClass() ), mediaSessionIntent );
		session.setActive( true );
		session.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );
		session.setCallback( new MediaSessionCompat.Callback() {
			@Override public void onPlay() { player.play(); }

			@Override public void onPause() { player.deferStop(); }

			@Override public void onStop() { player.stop(); }
		} );


		handler = new Handler( getMainLooper() );
	}

	//--------------------------------------------------------------------------

	@Nullable @Override public IBinder onBind( Intent intent ) {
		stopForeground( false );
		isBound = true;
		invalidateNotification();

		return new Binder();
	}

	//--------------------------------------------------------------------------

	@Override public boolean onUnbind( Intent intent ) {
		if ( player.isStreaming() ) {
			startForeground( NOTIFICATION_ID, makeNotification() );
		}
		isBound = false;
		return true;
	}

	//--------------------------------------------------------------------------

	@Override public void onRebind( Intent intent ) {
		stopForeground( false );
		isBound = true;
		invalidateNotification();
	}

	//--------------------------------------------------------------------------

	@Override public void onDestroy() {
		super.onDestroy();

		unbindService( this );
		unregisterReceiver( playPause_receiver );

		player.removeListener( this );
		session.release();
	}

	//--------------------------------------------------------------------------

	@Override public void onServiceConnected( ComponentName name, IBinder service ) {
		radioInfo_binder = (RadioInfoService.Binder)service;
		radioInfo_binder.addListener( this );
		if ( radioInfo_binder.getRadio() != null ) {
			onRadioUpdated( radioInfo_binder.getRadio() );
			onTimeUpdated( radioInfo_binder.getRadio() );
		}
	}

	//--------------------------------------------------------------------------

	@Override public void onServiceDisconnected( ComponentName name ) {
		radioInfo_binder.removeListener( this );
	}

	//--------------------------------------------------------------------------

	@Override public void onRadioUpdated( Radio radio ) {
		invalidateNotification();
		invalidateSessionMetadata();
		Picasso.with( this )
			.load( radio.dj.getAvatarUrl())
			.into( this );
	}

	//--------------------------------------------------------------------------

	@Override public void onTimeUpdated( Radio radio ) {
		invalidateNotification();
		invalidateSessionMetadata();
	}

	//--------------------------------------------------------------------------

	@Override public void onPlaybackStateChanged( @PlaybackState int newState ) {
		switch ( newState ) {
			case PLAYBACK_PLAYING:
			case PLAYBACK_BUFFERING:
				if ( isBound ) startService( new Intent( this, getClass() ));
				else startForeground( NOTIFICATION_ID, makeNotification() );
				break;

			default:
				if ( isBound ) stopSelf();
				else stopForeground( false );
				break;
		}

		for ( Listener l : LISTENERS ) l.onPlaybackStateChanged( newState );
		invalidateNotification();
		invalidateSessionState();
	}

	//--------------------------------------------------------------------------

	@Override public void onVisualizerCapture( Visualizer visualizer, double rms ) {
		handler.post( new VisCapRunnable( visualizer, rms ));
	}

	//--------------------------------------------------------------------------

	@Override public void onBitmapLoaded( Bitmap bitmap, Picasso.LoadedFrom from ) {
		djAvatar = bitmap;
		invalidateNotification();
		invalidateSessionMetadata();
	}

	//--------------------------------------------------------------------------

	@Override public void onBitmapFailed( Drawable errorDrawable ) {

	}

	//--------------------------------------------------------------------------

	@Override public void onPrepareLoad( Drawable placeHolderDrawable ) {

	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	private void invalidateNotification() {
		if ( isBound ) {
			NotificationManagerCompat.from( this ).cancel( NOTIFICATION_ID );

		} else {
			NotificationManagerCompat.from( this ).notify( NOTIFICATION_ID, makeNotification() );
		}
	}

	//--------------------------------------------------------------------------

	private Notification makeNotification() {
		final Radio radio = radioInfo_binder.getRadio();

		final NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();
		mediaStyle.setShowActionsInCompactView( 0 );
		mediaStyle.setMediaSession( session.getSessionToken() );

		NotificationCompat.Builder builder = new NotificationCompat.Builder( this );

		builder.setContentTitle( radioInfo_binder.getRadio().nowPlaying().meta );
		builder.setContentText( radio.dj.djname );
		builder.setSubText( getResources().getQuantityString( R.plurals.format_listeners, radio.listeners, radio.listeners ));
		builder.setSmallIcon( R.drawable.ic_notification );
		builder.setLargeIcon( djAvatar );
		builder.setStyle( mediaStyle );
		builder.setShowWhen( false );
		builder.setOngoing( player.isStreaming() );

		builder.setContentIntent( content_intent );
		builder.setDeleteIntent( stop_intent );

		if ( player.isPlaying() ) {
			builder.addAction( R.drawable.ic_pause_white_48dp, "", playPause_intent );

		} else {
			builder.addAction( R.drawable.ic_play_arrow_white_48dp, "", playPause_intent );
		}

		return builder.build();
	}

	//--------------------------------------------------------------------------

	private void invalidateSessionState() {
		final Radio radio = radioInfo_binder.getRadio();


		final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
			.setActions(
				PlaybackStateCompat.ACTION_PLAY       |
				PlaybackStateCompat.ACTION_PAUSE      |
				PlaybackStateCompat.ACTION_PLAY_PAUSE |
				PlaybackStateCompat.ACTION_STOP );


		int sessionState; float rate;
		switch ( player.getPlaybackState() ) {
			default:
			case PLAYBACK_STOPPED:   sessionState = PlaybackStateCompat.STATE_STOPPED;   rate = 0f; break;
			case PLAYBACK_BUFFERING: sessionState = PlaybackStateCompat.STATE_BUFFERING; rate = 0f; break;
			case PLAYBACK_PAUSED:    sessionState = PlaybackStateCompat.STATE_PAUSED;    rate = 0f; break;
			case PLAYBACK_PLAYING:   sessionState = PlaybackStateCompat.STATE_PLAYING;   rate = 1f; break;
		}

		stateBuilder.setState( sessionState, radio.nowPlaying().getElapsedSeconds() * 1000, rate );


		session.setPlaybackState( stateBuilder.build() );
	}

	//--------------------------------------------------------------------------

	private void invalidateSessionMetadata() {
		final Radio radio = radioInfo_binder.getRadio();

		MediaMetadataCompat.Builder metaBuilder = new MediaMetadataCompat.Builder()
			.putLong(   MediaMetadataCompat.METADATA_KEY_DURATION, radio.nowPlaying().length )
			.putString( MediaMetadataCompat.METADATA_KEY_MEDIA_ID, radio.nowPlaying().meta   )
			.putString( MediaMetadataCompat.METADATA_KEY_TITLE,    radio.nowPlaying().meta   )
			.putBitmap( MediaMetadataCompat.METADATA_KEY_ART,      djAvatar                  );
		session.setMetadata( metaBuilder.build() );
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	public class Binder extends android.os.Binder {
		//----------------------------------------------------------------------

		public void addListener( Listener l ) { LISTENERS.add( l ); }

		//----------------------------------------------------------------------

		public void removeListener( Listener l ) { LISTENERS.remove( l ); }

		//----------------------------------------------------------------------

		public void playPause() { player.playPause(); }

		//----------------------------------------------------------------------

		public @PlaybackState int getPlaybackState() { return player.getPlaybackState(); }

		//----------------------------------------------------------------------
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

	//==========================================================================
	private class VisCapRunnable implements Runnable {
		//----------------------------------------------------------------------

		private final Visualizer VIS;
		private final double RMS;

		//----------------------------------------------------------------------

		public VisCapRunnable( Visualizer vis, double rms ) {
			VIS = vis;
			RMS = rms;
		}

		//----------------------------------------------------------------------

		@Override public void run() {
			for ( Listener l : LISTENERS ) l.onVisualizerCapture( VIS, RMS );
		}

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

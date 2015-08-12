package io.r_a_d.android.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.r_a_d.android.R;
import io.r_a_d.android.drawable.PlayPauseDrawable;
import io.r_a_d.android.service.RadioInfoService;
import io.r_a_d.android.service.RadioPlaybackService;
import io.r_a_d.android.widget.PullDismissLayout;
import io.r_a_d.model.Radio;
import io.r_a_d.model.Song;
import prime.lib.android.radioplayer.RadioPlayer;

//==============================================================================
public class LastPlayedActivity
extends      AppCompatActivity
implements   RadioInfoService.Listener,
             RadioPlaybackService.Listener,
             PullDismissLayout.OnPullDismissListener {
	//--------------------------------------------------------------------------

	private List<Song> songs = Collections.emptyList();


	private ServiceConnection
		radioInfoService_connection,
		radioPlaybackService_connection;

	private RadioInfoService.Binder radioInfo_binder;
	private RadioPlaybackService.Binder radioPlayback_binder;

	private PlayPauseDrawable playPause_drawable;


	@Bind( R.id.pullDismiss ) PullDismissLayout pullDismiss;

	@Bind( R.id.nowPlaying_title ) TextView  nowPlaying_title;
	@Bind( R.id.playPause        ) ImageView playPause;

	@Bind( R.id.list ) RecyclerView list;

	//--------------------------------------------------------------------------

	@Override protected void onCreate( Bundle state ) {
		super.onCreate( state );


		setVolumeControlStream( AudioManager.STREAM_MUSIC );


		setContentView( R.layout.activity_lastplayed );
		ButterKnife.bind( this );

		pullDismiss.addOnPullDismissListener( this );
		pullDismiss.setPullEdges( Gravity.BOTTOM );

		playPause_drawable = new PlayPauseDrawable( getResources().getColor( R.color.white_a100 ));
		playPause.setImageDrawable( playPause_drawable );

		list.setLayoutManager( new LinearLayoutManager( this, LinearLayoutManager.VERTICAL, true ));
		list.setAdapter( new Adapter() );

		nowPlaying_title.setSelected( true );


		radioInfoService_connection = new ServiceConnection() {
			@Override public void onServiceConnected( ComponentName name, IBinder binder ) {
				radioInfo_binder = (RadioInfoService.Binder)binder;
				radioInfo_binder.addListener( LastPlayedActivity.this );
				if ( radioInfo_binder.getRadio() != null ) {
					onRadioUpdated( radioInfo_binder.getRadio() );
					onTimeUpdated( radioInfo_binder.getRadio() );
				}
			}

			@Override public void onServiceDisconnected( ComponentName name ) {
				radioInfo_binder.removeListener( LastPlayedActivity.this );
			}
		};

		radioPlaybackService_connection = new ServiceConnection() {
			@Override public void onServiceConnected( ComponentName name, IBinder service ) {
				radioPlayback_binder = (RadioPlaybackService.Binder)service;
				radioPlayback_binder.addListener( LastPlayedActivity.this );
				onPlaybackStateChanged( radioPlayback_binder.getPlaybackState() );
				playPause_drawable.endAnimation();
			}

			@Override public void onServiceDisconnected( ComponentName name ) {
				radioPlayback_binder.removeListener( LastPlayedActivity.this );
			}
		};
	}

	//--------------------------------------------------------------------------

	@Override public void onStart() {
		super.onStart();

		bindService( new Intent( this, RadioInfoService.class ), radioInfoService_connection, BIND_AUTO_CREATE );
		bindService( new Intent( this, RadioPlaybackService.class ), radioPlaybackService_connection, BIND_AUTO_CREATE );
	}

	//--------------------------------------------------------------------------

	@Override public void onStop() {
		super.onStop();

		unbindService( radioInfoService_connection );
		unbindService( radioPlaybackService_connection );
	}

	//--------------------------------------------------------------------------

	@Override public void onRadioUpdated( Radio radio ) {
		nowPlaying_title.setText( radio.nowPlaying().meta );
		songs = radio.lp;
		list.getAdapter().notifyDataSetChanged();
	}

	//--------------------------------------------------------------------------

	@Override public void onTimeUpdated( Radio radio ) {}

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
	}

	//--------------------------------------------------------------------------

	@Override public void onVisualizerCapture( Visualizer visualizer, double rms ) {

	}

	//--------------------------------------------------------------------------

	@Override public void onPullDismissReady( PullDismissLayout layout, int edgeGravity ) {

	}

	//--------------------------------------------------------------------------

	@Override public boolean onPullDismiss( PullDismissLayout layout, int edgeGravity ) {
		onNowPlayingClick();
		return false;
	}

	//--------------------------------------------------------------------------
	//--------------------------------------------------------------------------

	@OnClick( R.id.nowPlaying ) void onNowPlayingClick() {
		supportFinishAfterTransition();
	}

	//--------------------------------------------------------------------------

	@OnClick( R.id.playPause ) void onPlayPauseClick() {
		radioPlayback_binder.playPause();
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	private class Adapter extends RecyclerView.Adapter<ViewHolder> {
		//----------------------------------------------------------------------

		@Override public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
			final LayoutInflater inflater = LayoutInflater.from( parent.getContext() );
			return new ViewHolder( inflater.inflate( R.layout.listitem_playlist_song, parent, false ));
		}

		//----------------------------------------------------------------------

		@Override public void onBindViewHolder( ViewHolder holder, int position ) {
			holder.title.setText( songs.get( position ).meta );

			final Song song = songs.get( position );
			final long now = System.currentTimeMillis();
			final CharSequence relativeTime = DateUtils.getRelativeTimeSpanString( song.timestamp * 1000, now, 0 );

			holder.time.setText( relativeTime );

			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
				if ( position == 0 ) holder.title.setTransitionName( "lastPlayed_title" );
				else                 holder.title.setTransitionName( null );
			}
		}

		//----------------------------------------------------------------------

		@Override public int getItemCount() {
			return songs.size();
		}

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------

	//==========================================================================
	class ViewHolder extends RecyclerView.ViewHolder {
		//----------------------------------------------------------------------

		@Bind( R.id.title ) TextView title;
		@Bind( R.id.time  ) TextView time;

		//----------------------------------------------------------------------

		public ViewHolder( View itemView ) {
			super( itemView );
			ButterKnife.bind( this, itemView );
		}

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

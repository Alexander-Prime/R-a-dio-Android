package io.r_a_d.android.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.r_a_d.api.RadioRestInterface;
import io.r_a_d.api.response.RestResponse;
import io.r_a_d.model.Meta;
import io.r_a_d.model.Radio;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

//==============================================================================
public class RadioInfoService extends Service {
	//--------------------------------------------------------------------------

	// One-second clock interval
	private final Runnable TIME_UPDATER = new Runnable() {
		@Override public void run() {
			// Move on if we're at the end of the song
			if ( radio.nowPlaying().isFinishedInterval() ) {
				refreshRadio();
			}

			dispatchOnTimeUpdated( radio );

			handler.postDelayed( TIME_UPDATER, 1000 );
		}
	};

	private final List<Listener> LISTENERS = new ArrayList<>();
	private final RadioRestInterface REST = new RestAdapter.Builder()
		.setEndpoint( "https://r-a-d.io/api" )
		.setLogLevel( RestAdapter.LogLevel.FULL )
		.build()
		.create( RadioRestInterface.class );

	private Radio radio;
	private Meta meta;
	private Handler handler;

	//--------------------------------------------------------------------------

	@Override public void onCreate() {
		super.onCreate();

		handler = new Handler();
	}

	//--------------------------------------------------------------------------

	@Nullable @Override public IBinder onBind( Intent intent ) {
		refreshRadio();

		return new Binder();
	}

	//--------------------------------------------------------------------------

	@Override public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks( TIME_UPDATER );
	}

	//--------------------------------------------------------------------------

	private void refreshRadio() {
		// R/a/dio has most important information available through one method
		REST.getStatus( new Callback<RestResponse<Radio>>() {
			@Override public void success( RestResponse<Radio> streamResponse, Response response ) {
				radio = streamResponse.main;
				meta  = streamResponse.meta;
				dispatchOnStreamUpdated( radio );

				// (Re)start timer
				handler.removeCallbacks( TIME_UPDATER );
				handler.postDelayed( TIME_UPDATER, 1000 );
			}

			@Override public void failure( RetrofitError error ) {
				error.printStackTrace();
			}
		} );
	}

	//--------------------------------------------------------------------------

	private void dispatchOnStreamUpdated( Radio radio ) {
		for ( Listener l : LISTENERS ) l.onRadioUpdated( radio );
	}

	//--------------------------------------------------------------------------

	private void dispatchOnTimeUpdated( Radio radio ) {
		for ( Listener l : LISTENERS ) l.onTimeUpdated( radio );
	}

	//--------------------------------------------------------------------------

	//==========================================================================
	public class Binder extends android.os.Binder {
		//----------------------------------------------------------------------

		public void addListener( Listener l ) { LISTENERS.add( l ); }

		//----------------------------------------------------------------------

		public void removeListener( Listener l ) { LISTENERS.remove( l ); }

		//----------------------------------------------------------------------

		public void refresh() { refreshRadio(); }

		//----------------------------------------------------------------------

		public Radio getRadio() { return radio; }

		//----------------------------------------------------------------------

		public Uri getStreamUri() { return Uri.parse( meta.stream ); }

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------

	//==========================================================================
	public interface Listener {
		//----------------------------------------------------------------------

		void onRadioUpdated( Radio radio );
		void onTimeUpdated( Radio radio );

		//----------------------------------------------------------------------
	}
	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

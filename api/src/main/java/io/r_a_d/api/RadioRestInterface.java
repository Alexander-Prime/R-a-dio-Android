package io.r_a_d.api;

import java.util.List;

import io.r_a_d.api.response.RestResponse;
import io.r_a_d.model.News;
import io.r_a_d.model.Radio;
import io.r_a_d.model.Song;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

//==============================================================================
public interface RadioRestInterface {
	//--------------------------------------------------------------------------

	@GET( "/" )
	void getStatus( Callback<RestResponse<Radio>> callback );


	// Nothing below this line actually works, although it's documented
	//--------------------------------------------------------------------------

	@GET( "/last-played" )
	void getLastPlayed( Callback<RestResponse<List<Song>>> callback );

	@GET( "/last-played" )
	void getLastPlayed( @Query( "limit" ) int limit, Callback<RestResponse<List<Song>>> callback  );

	@GET( "/last-played" )
	void getLastPlayed( @Query( "limit" ) int limit, @Query( "offset" ) int offset, Callback<RestResponse<List<Song>>> callback  );

	//--------------------------------------------------------------------------

	@GET( "/queue" )
	void getQueue( Callback<RestResponse<List<Song>>> callback );

	//--------------------------------------------------------------------------

	@GET( "/news" )
	void getNews( Callback<RestResponse<List<News>>> callback );

	@GET( "/news" )
	void getNews( @Query( "limit" ) int limit, Callback<RestResponse<List<News>>> callback  );

	@GET( "/news" )
	void getNews( @Query( "limit" ) int limit, @Query( "offset" ) int offset, Callback<RestResponse<List<News>>> callback  );


	@GET( "/news/{id}" )
	void getNews( @Path( "id" ) long id, Callback<RestResponse<News>> callback );

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

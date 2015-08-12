package io.r_a_d.model;

//==============================================================================
public class DJ {
	//--------------------------------------------------------------------------

	public int
		id;

	public String
		djname,
		role,
		djcolor;

	public int
		visible;

	//--------------------------------------------------------------------------

	public String getAvatarUrl() {
		return String.format( "https://r-a-d.io/api/dj-image/%s", id );
	}

	//--------------------------------------------------------------------------

	public int getColor() {
		final String[] components = djcolor.split( "\\s" );

		return 0xff000000 |
			Integer.parseInt( components[0] ) << 16 |
			Integer.parseInt( components[1] ) <<  8 |
			Integer.parseInt( components[2] );
	}

	//--------------------------------------------------------------------------
}
//------------------------------------------------------------------------------

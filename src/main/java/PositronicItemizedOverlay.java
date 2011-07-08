// Glue code to deal with the Google Maps API from Scala; some
// features are awkward.  

package org.positronicnet.maps;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import android.graphics.drawable.Drawable;

abstract public class PositronicItemizedOverlay< T extends OverlayItem>
  extends ItemizedOverlay< T >
{
  public static final int MARKER_CENTERED = 0;
  public static final int MARKER_CENTERED_ABOVE = 1;

  private static Drawable placeMarker( Drawable d, int how ) {
    switch( how ) {
    case MARKER_CENTERED:       return boundCenter( d );
    case MARKER_CENTERED_ABOVE: return boundCenterBottom( d );
    default: throw( new IllegalArgumentException( "Bad marker location spec" ));
    }
  }

  public PositronicItemizedOverlay( Drawable d, int markerPlacement ) {
    super( placeMarker( d, markerPlacement ));
  }
}
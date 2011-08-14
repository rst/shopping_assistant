// Glue code to deal with variations on the Google Maps API from Scala; 
// this variant wraps Jeff Gilfelt's BalloonItemizedOverlay. 

package org.positronicnet.maps;

import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.MapView;
import android.graphics.drawable.Drawable;


abstract public class PositronicBalloonItemizedOverlay< T extends OverlayItem>
  extends BalloonItemizedOverlay< T >
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

  public PositronicBalloonItemizedOverlay( MapView mapView, Drawable d, int markerPlacement ) {
    super( placeMarker( d, markerPlacement ), mapView );
   }
}
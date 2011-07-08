package org.positronicnet.demo.shopping

import com.google.android.maps.MapActivity
import com.google.android.maps.OverlayItem
import com.google.android.maps.GeoPoint

import org.positronicnet.maps.PositronicItemizedOverlay
import org.positronicnet.ui.PositronicActivityHelpers
import android.os.Bundle
import android.graphics.drawable.Drawable;

class MyOverlay( d: Drawable )
 extends PositronicItemizedOverlay[OverlayItem](d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  private val items = Array( 
    new OverlayItem( new GeoPoint(19240000,-99120000), 
                     "Hola, Mundo!", "I'm in Mexico City!"),
    new OverlayItem( new GeoPoint(35410000, 139460000),
                     "Sekai, konichiwa!", "I'm in Japan!")
  )

  override def size = items.size
  override def createItem( i: Int ):OverlayItem = items( i )

  this.populate()
}



class TodoMapActivity
  extends MapActivity with PositronicActivityHelpers with ViewFinder
{
  onCreate { 
    setContentView( R.layout.map ) 
    val icon = getResources.getDrawable( android.R.drawable.btn_star_big_on )

    findView( TR.mapview ).setBuiltInZoomControls( true )
    findView( TR.mapview ).getOverlays.add( new MyOverlay( icon ))
    ()
  }

  def isRouteDisplayed = false
}

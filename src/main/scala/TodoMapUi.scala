package org.positronicnet.demo.shopping

import com.google.android.maps.MapActivity
import com.google.android.maps.MapView
import com.google.android.maps.Overlay
import com.google.android.maps.OverlayItem
import com.google.android.maps.GeoPoint

import org.positronicnet.maps.PositronicItemizedOverlay
import org.positronicnet.ui.PositronicActivityHelpers

import android.os.Bundle
import android.graphics.drawable.Drawable
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import android.util.Log

class DummyItemOverlay( ctx: Context, d: Drawable )
 extends PositronicItemizedOverlay[OverlayItem](d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  private val items = Array( 
    new OverlayItem( new GeoPoint(19240000,-99120000), 
                     "Hola, Mundo!", "I'm in Mexico City!"),
    new OverlayItem( new GeoPoint(35410000, 139460000),
                     "Sekai, konichiwa!", "I'm in Japan!")
  )

  def size = items.size
  def createItem( i: Int ):OverlayItem = items( i )

  override def onTap( i: Int ): Boolean = {
    val dialog = new AlertDialog.Builder( ctx )
    dialog.setTitle( items(i).getTitle )
    dialog.setMessage( items(i).getSnippet )
    dialog.show
    return true;
  }

  this.populate()
}

class NoteTapOverlay( activity: TodoMapActivity )
  extends Overlay
{
  override def onTap( pt: GeoPoint, mv: MapView ) = activity.unclaimedTap( pt )
}

class TodoMapActivity
  extends MapActivity with PositronicActivityHelpers with ViewFinder
{
  onCreate { 
    setContentView( R.layout.map ) 
    val icon = getResources.getDrawable( android.R.drawable.btn_star_big_on )

    findView( TR.mapview ).getOverlays.add( new NoteTapOverlay( this ))
    findView( TR.mapview ).getOverlays.add( new DummyItemOverlay( this, icon ))
    findView( TR.mapview ).setBuiltInZoomControls( true )
  }

  def isRouteDisplayed = false

  def unclaimedTap( pt: GeoPoint ): Boolean = {
    
    Log.d( "XXX", "Got tap at " + pt.toString )

    Toast.makeText( this, "Tap at " + pt.toString, Toast.LENGTH_SHORT ).show
    return true
  }
}

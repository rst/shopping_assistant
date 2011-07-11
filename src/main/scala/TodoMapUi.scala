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

class EditPlacesOverlay( activity: TodoMapActivity, 
                         list: TodoList, 
                         places: IndexedSeq[ TodoPlace ],
                         d: Drawable )
 extends PositronicItemizedOverlay[OverlayItem](d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  val defaultDescription = "A " + list.name

  var items:  IndexedSeq[OverlayItem] = 
    for ( place <- places )
    yield new OverlayItem( new GeoPoint( place.latitude, place.longitude ),
                           defaultDescription,
                           "No item count on edit screen!" )

  populate

  def size = items.size
  def createItem( i: Int ):OverlayItem = items( i )

  override def onTap( i: Int ): Boolean = {
    list.deletePlace( places( i ) )
    return true
  }
}

class NoteTapOverlay( activity: TodoMapActivity )
  extends Overlay
{
  override def onTap( pt: GeoPoint, mv: MapView ) = activity.unclaimedTap( pt )
}

class TodoMapActivity
  extends MapActivity with PositronicActivityHelpers with ViewFinder
{
  lazy val mapView = findView( TR.mapview )
  lazy val icon = getResources.getDrawable( android.R.drawable.btn_star_big_on )
  var editingList: TodoList = null;

  onCreate { 
    setContentView( R.layout.map ) 

    val listChooser = findView( TR.list_chooser )

    findView( TR.mapview ).setBuiltInZoomControls( true )

    listChooser.setAdapter( new TodosAdapter( this ))
    listChooser.onItemSelected{ (view, posn, id) =>
      val selectedItem = listChooser.getAdapter.getItem( posn )
      prepareToEdit( selectedItem.asInstanceOf[ TodoList ] )
    }
  }

  def isRouteDisplayed = false

  def prepareToEdit( list: TodoList ) = {
    if (editingList != list) {
      editingList = list
      onChangeTo( list.places ) { places =>
        this.runOnUiThread{ setOverlaysForEdit( list, places )}
      }
    }
  }

  def setOverlaysForEdit( list: TodoList, places: IndexedSeq[TodoPlace] ):Unit = {
    // Adding or removing overlay items from a preexisting ItemizedOverlay
    // can get really fussy.  So, we just clear 'em out and redo from scratch,
    // for the moment.

    mapView.getOverlays.clear
    mapView.getOverlays.add( new NoteTapOverlay( this ))
    mapView.getOverlays.add( new EditPlacesOverlay( this, list, places, icon ))
    mapView.invalidate
  }

  def unclaimedTap( pt: GeoPoint ): Boolean = {
    if (editingList == null)
      return false;
    editingList.addPlace( pt.getLatitudeE6, pt.getLongitudeE6 )
    return true
  }
}

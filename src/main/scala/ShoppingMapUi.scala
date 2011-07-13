package org.positronicnet.demo.shopping

import com.google.android.maps.MapActivity
import com.google.android.maps.MapView
import com.google.android.maps.Overlay
import com.google.android.maps.OverlayItem
import com.google.android.maps.GeoPoint

import org.positronicnet.maps.PositronicItemizedOverlay
import org.positronicnet.maps.PositronicBalloonItemizedOverlay
import org.positronicnet.ui.PositronicActivityHelpers
import org.positronicnet.ui.IndexedSeqAdapter

import android.os.Bundle
import android.graphics.drawable.Drawable
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import android.widget.ImageView
import android.util.Log

// Overlay for just viewing a list

class TodoListPresentationOverlay( map: MapView, list: TodoList, d: Drawable ) 
  extends PositronicBalloonItemizedOverlay[OverlayItem](map, d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  val defaultDescription = "A " + list.name

  val items:  IndexedSeq[OverlayItem] = 
    for ( place <- list.places.value )
    yield new OverlayItem( new GeoPoint( place.latitude, place.longitude ),
                           defaultDescription, null )

  def size = items.size
  def createItem( i: Int ):OverlayItem = items( i )

  populate
}

// Overlays for edit

class TodoEditBgOverlay( list: TodoList, 
                         places: IndexedSeq[ TodoPlace ],
                         d: Drawable )
  extends PositronicItemizedOverlay[OverlayItem](d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  val defaultDescription = "A " + list.name

  val items:  IndexedSeq[OverlayItem] = 
    for ( place <- places )
    yield new OverlayItem( new GeoPoint( place.latitude, place.longitude ),
                           defaultDescription, null )

  def size = items.size
  def createItem( i: Int ):OverlayItem = items( i )

  populate
}

class EditPlacesOverlay( activity: TodoMapActivity, 
                         list: TodoList, 
                         places: IndexedSeq[ TodoPlace ],
                         d: Drawable )
  extends TodoEditBgOverlay( list, places, d )
{
  override def onTap( i: Int ): Boolean = {
    list.deletePlace( places( i ) )
    return true
  }
}

// Overlay to soak up unclaimed taps on the map.

class NoteTapOverlay( activity: TodoMapActivity )
  extends Overlay
{
  override def onTap( pt: GeoPoint, mv: MapView ) = activity.unclaimedTap( pt )
}

class TodoMapActivity
  extends MapActivity with PositronicActivityHelpers with ViewFinder
{
  useOptionsMenuResource( R.menu.map_menu )

  lazy val mapView = findView( TR.mapview )
  lazy val colorSpinner = findView( TR.color_spinner )
  lazy val listChooser = findView( TR.list_chooser )
  lazy val listsAdapter = new TodosAdapter( this )

  lazy val icons = TodoMaps.icons( this )
  var editingList: TodoList = null
  var editingMode = false

  onCreate { 

    setContentView( R.layout.map ) 
    findView( TR.mapview ).setBuiltInZoomControls( true )
    startViewing

    listChooser.setAdapter( listsAdapter )
    listChooser.onItemSelected{ (view, posn, id) => editSelectedList }

    colorSpinner.setAdapter( new ListIconChoiceAdapter( this ) )
    colorSpinner.onItemSelected{ (view, posn, id) => 
      if (editingList != null) {
        TodoLists.setListIconIdx( editingList, posn )
        setOverlaysForEdit( editingList, editingList.places.value )
      }
    }

    onOptionsItemSelected( R.id.edit ){ startEdit }
    onOptionsItemSelected( R.id.view ){ startViewing }
    onOptionsItemSelected( R.id.editlists ) {
      startActivity( new Intent( this, classOf[ TodosActivity ]) )
    }
  }

  def isRouteDisplayed = false

  def startEdit = { 

    findView( TR.edit_controls ).setVisibility( View.VISIBLE )
    setTitle( R.string.edit_map )

    editingMode = true
    editingList = null                  // force redisplay
    editSelectedList 
  }

  def startViewing = {

    findView( TR.edit_controls ).setVisibility( View.GONE )
    setTitle( R.string.show_map )

    editingMode = false
    setOverlaysForViews
  }

  def editSelectedList = {
    if (editingMode) {
      val list = listChooser.getSelectedItem.asInstanceOf[ TodoList ]
      if (editingList != list && editingMode) {
        editingList = list
        colorSpinner.setSelection( list.iconIdx, false )
        onChangeTo( list.places ) { places =>
          this.runOnUiThread{ setOverlaysForEdit( list, places )}}
      }
    }
  }

  def setOverlaysForEdit( listToEdit: TodoList, 
                          places: IndexedSeq[TodoPlace] ):Unit = 
  {
    // Adding or removing overlay items from a preexisting ItemizedOverlay
    // can get really fussy.  So, we just clear 'em out and redo from scratch,
    // for the moment.

    mapView.getOverlays.clear
    mapView.getOverlays.add( new NoteTapOverlay( this ))

    for (i <- Range( 0, listsAdapter.getCount )) {
      val list = listsAdapter.getItem( i ).asInstanceOf[ TodoList ]
      if (list.id != listToEdit.id) 
        mapView.getOverlays.add( 
          new TodoEditBgOverlay( list, list.places.value,
                                 icons( list.iconIdx ).small ))
    }

    mapView.getOverlays.add( 
      new EditPlacesOverlay( this, listToEdit, places, 
                             icons( listToEdit.iconIdx ).large ))
    mapView.invalidate
  }

  def setOverlaysForViews = {
    mapView.getOverlays.clear
    for (i <- Range( 0, listsAdapter.getCount )) {
      val list = listsAdapter.getItem( i ).asInstanceOf[ TodoList ]
      val icon = if (list.numUndoneItems.value > 0) icons( list.iconIdx ).large
                 else icons( list.iconIdx ).small
      mapView.getOverlays.add( 
        new TodoListPresentationOverlay( mapView, list, icon ))
    }
    mapView.invalidate
  }

  def unclaimedTap( pt: GeoPoint ): Boolean = {
    if (editingList == null)
      return false;
    editingList.addPlace( pt.getLatitudeE6, pt.getLongitudeE6 )
    return true
  }
}

object TodoMaps {

  case class IconSet( small: Drawable, large: Drawable )

  val numIcons = 3 

  def icons( ctx: Context ):IndexedSeq[ IconSet ] = {

    val rsrc = ctx.getResources
    val drawable = ( id: Int ) => rsrc.getDrawable( id )

    return IndexedSeq( IconSet( drawable( R.drawable.bluecircle ), 
                                drawable( R.drawable.bluecirclebig )),
                       IconSet( drawable( R.drawable.redcircle ),  
                                drawable( R.drawable.redcirclebig )),
                       IconSet( drawable( R.drawable.greencircle ),
                                drawable( R.drawable.greencirclebig )))
  }
}

class ListIconChoiceAdapter( activity: TodoMapActivity )
 extends IndexedSeqAdapter( TodoMaps.icons( activity ),
                            itemViewResourceId = R.layout.image_view )
{
  override def bindView( view: View, iconSet: TodoMaps.IconSet ) =
    view.asInstanceOf[ ImageView ].setImageDrawable( iconSet.large )
}
                            

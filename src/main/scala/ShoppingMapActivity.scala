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

import org.positronicnet.notifications._
import org.positronicnet.notifications.Actions._

import org.positronicnet.orm._
import org.positronicnet.orm.Actions._

import android.os.Bundle
import android.graphics.drawable.Drawable
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.Menu
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.util.Log

// Map-based portions of the Shopping-list UI:  adding shops
// and seeing which ones have current items.
//
// A lot of the messiness here is because this activity is
// really doing double duty, as *both* the "edit" and "view"
// screens.  Which we do because we're forced; the Android
// mapping toolkit only supports a single MapActivity per
// process.

class ShoppingMapActivity
  extends MapActivity with PositronicActivityHelpers with ViewFinder
{
  useOptionsMenuResource( R.menu.map_menu )

  lazy val mapView = findView( TR.mapview )
  lazy val colorSpinner = findView( TR.color_spinner )
  lazy val listChooser = findView( TR.list_chooser )
  lazy val listsAdapter = new ShoppingListsAdapter( this )

  // Basic UI wiring.

  onCreate { 

    useAppFacility( ShoppingDb ) // Open DB; arrange to close on destroy
    useAppFacility( ProxAlertManagement )

    setContentView( R.layout.map ) 
    findView( TR.mapview ).setBuiltInZoomControls( true )
    startViewing

    listChooser.setAdapter( listsAdapter )
    listChooser.onItemSelected{ (view, posn, id) => editSelectedList }

    colorSpinner.setAdapter( new ListIconChoiceAdapter( this ) )
    colorSpinner.onItemSelected{ (view, posn, id) => iconSelected( posn ) }

    onOptionsItemSelected( R.id.edit ){ startEdit }
    onOptionsItemSelected( R.id.view ){ startViewing }
    onOptionsItemSelected( R.id.editlists ) {
      startActivity( new Intent( this, classOf[ ShoppingListsActivity ]) )
    }
  }

  override def onPrepareOptionsMenu( menu: Menu ) = {
    menu.findItem( R.id.edit ).setVisible( !editingMode )
    menu.findItem( R.id.view ).setVisible(  editingMode )
    true
  }

  // Handling "view mode"

  def startViewing = {

    setTitle( R.string.show_map )

    endEdit                             // if we had one in progress
    setOverlaysForViews
  }

  def setOverlaysForViews = {

    mapView.getOverlays.clear

    ShoppingLists ! Fetch { lists => 
      for (list <- lists) {
        list.undoneItems.count ! Fetch{ numUndone => {
          val icon = if (numUndone > 0) ShoppingIcons.large( list, this )
                     else ShoppingIcons.small( list, this )

          if (!editingMode) {             // paranoia about race conditions
            mapView.getOverlays.add( 
              new ShopPresentationOverlay( mapView, list, icon ))
          }
        }}
      }
    }
  }

  // Edit-mode and state maintenance for it.

  var editingMode = false
  var editingList: ShoppingList = null
  var editOverlay: EditShopsOverlay = null

  def startEdit = { 
    findView( TR.edit_controls ).setVisibility( View.VISIBLE )
    setTitle( R.string.edit_map )

    editingMode = true
    editingList = null                  // force redisplay
    editSelectedList 

    mapView.onFreeDoubleTap{ pt =>
      val shop = editingList.shops.create.setLocation( pt.getLatitudeE6,
                                                       pt.getLongitudeE6 )
      editingList.shops ! Save( shop )
    }

    // Double-tap on a shop handled in the overlay itself.
  }

  def endEdit = 
    if (editingMode) {
      if (editOverlay != null) editOverlay.stopEdit
      editingMode = false
      editingList = null
      editOverlay = null
      findView( TR.edit_controls ).setVisibility( View.GONE )
      mapView.onFreeDoubleTap{ pt => null }
    }

  onDestroy{ endEdit }

  def iconSelected( iconIdx: Int ) = 
    if (editingList != null) {
      editingList = editingList.setIconIdx( iconIdx )
      ShoppingLists ! Save( editingList )
      setOverlaysForEdit( editingList )
    }

  def editSelectedList = {
    if (editingMode) {
      val list = listChooser.getSelectedItem.asInstanceOf[ ShoppingList ]
      if (editingList != list && editingMode) {
        editingList = list
        colorSpinner.setSelection( list.iconIdx, false )
        setOverlaysForEdit( list )
      }
    }
  }

  def setOverlaysForEdit( listToEdit: ShoppingList ):Unit =  {

    mapView.getOverlays.clear

    for (i <- Range( 0, listsAdapter.getCount )) {
      val list = listsAdapter.getItem( i ).asInstanceOf[ ShoppingList ]
      if (list.id != listToEdit.id) 
        mapView.getOverlays.add( 
          new EditShopsBgOverlay( mapView, list, 
                                  ShoppingIcons.small( list, this )))
    }

    editOverlay = 
      new EditShopsOverlay( mapView, listToEdit, 
                            ShoppingIcons.large( listToEdit, this ))

    mapView.getOverlays.add( editOverlay )
  }

  // Persist last-viewed-position state ... actually into shared prefs.

  lazy val prefs = getPreferences( Context.MODE_PRIVATE )

  override def saveInstanceState(b : Bundle) = {
    val editor = prefs.edit
    editor.putBoolean( "HavePosition", true )
    editor.putInt( "LastLongitude", mapView.getMapCenter.getLongitudeE6 )
    editor.putInt( "LastLatitude",  mapView.getMapCenter.getLatitudeE6 )
    editor.putInt( "LastZoom", mapView.getZoomLevel )
    editor.commit
  }
  
  override def restoreInstanceState(b: Bundle) = {
    if ( prefs.getBoolean( "HavePosition", false ) ) {
      val controller = mapView.getController
      val latitude  = prefs.getInt( "LastLatitude",  -1 )
      val longitude = prefs.getInt( "LastLongitude", -1 )
      controller.setCenter( new GeoPoint( latitude, longitude ))
      controller.setZoom( prefs.getInt( "LastZoom", 0 ))
    }
  }

  // Tell Google we're not using routes.

  def isRouteDisplayed = false
}

class ListIconChoiceAdapter( activity: ShoppingMapActivity )
  extends IndexedSeqAdapter( ShoppingIcons.iconResIds,
                             itemViewResourceId = R.layout.image_view )
  with SpinnerAdapter
{
  override def bindView( view: View, iconSet: ShoppingIcons.IconSet ) = {
    val drawable = activity.getResources.getDrawable( iconSet.largeResId )
    view.asInstanceOf[ ImageView ].setImageDrawable( drawable )
  }
}

// Map Overlays.  (You'll notice a certain duplication between the
// presentation and edit overlay base classes.  Unfortunately, it's
// hard to wrap these up in a trait, due to Scala "implementation
// restrictions" in accessing protected base class members "populate"
// and "setLastFocusedIndex" from a trait that gets mixed in...)

// Overlay for just viewing a list

class ShopPresentationOverlay( map: MapView, list: ShoppingList, d: Drawable ) 
  extends PositronicBalloonItemizedOverlay[OverlayItem](map, d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  val defaultDescription = "A " + list.name
  var shops:  IndexedSeq[Shop] = IndexedSeq.empty

  def size = shops.size
  def createItem( i: Int ):OverlayItem = 
    new OverlayItem( new GeoPoint( shops(i).latitude, shops(i).longitude ),
                     defaultDescription, null )

  list.shops ! Fetch{ shops =>
    this.shops = shops
    this.populate
    map.invalidate
  }
}

// Overlays for edit

class EditShopsBgOverlay( map: MapView, list: ShoppingList, d: Drawable )
  extends PositronicItemizedOverlay[OverlayItem](d, PositronicItemizedOverlay.MARKER_CENTERED)
{
  val defaultDescription = "A " + list.name
  var shops: IndexedSeq[Shop] = IndexedSeq.empty

  def size = shops.size
  def createItem( i: Int ):OverlayItem = 
    new OverlayItem( new GeoPoint( shops(i).latitude, shops(i).longitude ),
                     defaultDescription, null )

  list.shops ! Fetch{ shops => resetOverlay( shops ) }

  def resetOverlay( newShops: IndexedSeq[ Shop ] ) = {
    shops = newShops
    setLastFocusedIndex( -1 )           // so we don't die if the list shrank
    populate
    map.invalidate
  }
}

class EditShopsOverlay( map: MapView, list: ShoppingList, d: Drawable )
  extends EditShopsBgOverlay( map, list, d )
  with DoubleTapDetection[ OverlayItem ]
{
  list.shops ! AddWatcher( this ){ resetOverlay( _ ) }

  def stopEdit = list.shops ! StopWatcher( this )

  onDoubleTap{ idx => list.shops ! Delete( shops( idx ) ) }
}

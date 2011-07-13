package org.positronicnet.demo.shopping

import org.positronicnet.ui.IndexedSeqAdapter
import org.positronicnet.ui.PositronicDialog
import org.positronicnet.ui.PositronicActivity
import org.positronicnet.ui.PositronicActivityHelpers
import org.positronicnet.ui.CursorSourceAdapter

import org.positronicnet.util.ChangeNotifications
import org.positronicnet.db.PositronicCursor

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ContextMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.SpinnerAdapter
import android.graphics.Paint
import android.graphics.Canvas

// Adapter to wire up ShoppingList changes to the UI.
//
// Registers with the "source" (our ShoppingLists singleton) to be
// notified whenever its underlying data set is changed (or reloaded),
// so long as the activity is running.
//
// (MapActivities want to use this as well, so the constructor takes
// any activity with our ActivityHelpers trait.)

class ShoppingListsAdapter( activity: PositronicActivityHelpers )
 extends CursorSourceAdapter( activity, 
                              source = ShoppingLists.lists,
                              converter = ShoppingList.fromCursor(_),
                              itemViewResourceId = R.layout.shoppinglist_row )
 with SpinnerAdapter
{
  def bindItem( view: View, list: ShoppingList ) =
    view.asInstanceOf[ TextView ].setText( list.name )
}

// Activity that uses it:

class ShoppingListsActivity 
 extends PositronicActivity( layoutResourceId = R.layout.all_lists ) 
 with ViewFinder 
{
  lazy val listsView = findView( TR.listsView )
  lazy val renameDialog = new EditStringDialog( this )

  onCreate {

    useOptionsMenuResource( R.menu.lists_view_menu )
    useContextMenuResource( R.menu.lists_context_menu )

    // Wire listsView to the database

    useAppFacility( ShoppingDb ) // Open DB; arrange to close on destroy
    listsView.setAdapter( new ShoppingListsAdapter( this ))

    // Listen for events on widgets

    listsView.onItemClick { (view, posn, id) => viewListAt( posn ) }

    findView( TR.addButton ).onClick { doAdd }
    findView( TR.newListName ).onKey( KeyEvent.KEYCODE_ENTER ){ doAdd }

    onOptionsItemSelected( R.id.undelete ) { doUndelete }
    onOptionsItemSelected( R.id.maps ) {
      startActivity( new Intent( this, classOf[ ShoppingMapActivity ] ))
    }

    registerForContextMenu( listsView )

    onContextItemSelected( R.id.rename ){ 
      (menuInfo, view) => doRename( getContextItem( menuInfo, view ))
    }
    onContextItemSelected( R.id.delete ){ 
      (menuInfo, view) => doDelete( getContextItem( menuInfo, view ))
    }
  }

  // Determining relevant context for the ContextMenu

  def getContextItem( menuInfo: ContextMenu.ContextMenuInfo, view: View ) =
    listsView.selectedContextMenuItem( menuInfo ).asInstanceOf[ ShoppingList ]

  // Running UI commands

  def doAdd = {
    val str = findView( TR.newListName ).getText.toString
    if ( str != "" ) {
      ShoppingLists.addList( name = str )
      findView( TR.newListName ).setText("")
    }
  }

  def doRename( list: ShoppingList ) =
    renameDialog.doEdit( list.name ){ 
      ShoppingLists.setListName( list, _ )
    }

  def doDelete( list: ShoppingList ) = {
    ShoppingLists.removeList( list )
    toast( R.string.list_deleted, Toast.LENGTH_LONG )
  }

  def doUndelete = { 
    if (ShoppingLists.numDeletedLists.value > 0) ShoppingLists.undelete
    else toast( R.string.undeletes_exhausted )
  }

  def viewListAt( posn: Int ) {
    val intent = new Intent( this, classOf[ ShoppingListActivity ] )
    val theList = listsView.getAdapter.getItem(posn).asInstanceOf[ShoppingList]
    ShoppingList.intoIntent( theList, intent )
    startActivity( intent )
  }
}

// The activity's generic support dialog (used by the other activity as well).

class EditStringDialog( base: PositronicActivity )
 extends PositronicDialog( base, layoutResourceId = R.layout.dialog ) 
 with ViewFinder 
{
  val editTxt = findView( TR.dialogEditText )
  var saveHandler: ( String => Unit ) = null

  editTxt.onKey( KeyEvent.KEYCODE_ENTER ){ doSave; dismiss }

  findView( TR.cancelButton ).onClick { dismiss }
  findView( TR.saveButton ).onClick { doSave; dismiss }
  
  def doSave = saveHandler( editTxt.getText.toString )

  def doEdit( str: String )( handler: String => Unit ) = { 
    editTxt.setText( str )
    saveHandler = handler
    show
  }
}

// The activity which manages an individual shopping list's items.

class ShoppingListActivity 
 extends PositronicActivity( layoutResourceId = R.layout.shopping_one_list ) 
 with ViewFinder 
{
  var theList: ShoppingList = null

  lazy val newItemText = findView( TR.newItemText )
  lazy val listItemsQuery = theList.itemsQuery( initialShowDone = true )
  lazy val listItemsView = findView( TR.listItemsView )
  lazy val editDialog = new EditStringDialog( this )

  onCreate{

    useOptionsMenuResource( R.menu.items_view_menu )
    useContextMenuResource( R.menu.item_context_menu )

    // Setup --- get list out of our Intent, and hook up the listItemsView

    theList = ShoppingList.fromIntent( getIntent )
    setTitle( "Todo for: " + theList.name )

    useAppFacility( ShoppingDb )
    listItemsView.setAdapter( new ShopItemsAdapter( this, listItemsQuery ) )

    // Event handlers...

    listItemsView.onItemClick {( view, posn, id ) =>
      toggleDone( getDisplayedItem( posn )) }

    findView( TR.addButton ).onClick { doAdd }
    newItemText.onKey( KeyEvent.KEYCODE_ENTER ){ doAdd }

    onOptionsItemSelected( R.id.delete_where_done ) { deleteWhereDone }
    onOptionsItemSelected( R.id.undelete ) { undelete }

    registerForContextMenu( listItemsView )

    onContextItemSelected( R.id.edit ){ 
      (menuInfo, view) => doEdit( getContextItem( menuInfo, view ))
    }
    onContextItemSelected( R.id.toggledone ){ 
      (menuInfo, view) => toggleDone( getContextItem( menuInfo, view ))
    }
  }

  // Finding target items for listItemsView taps (including the ContextMenu)

  def getContextItem( menuInfo: ContextMenu.ContextMenuInfo, view: View ) =
    listItemsView.selectedContextMenuItem( menuInfo ).asInstanceOf[ ShopItem ]

  def getDisplayedItem( posn: Int ) = 
    listItemsView.getAdapter.getItem( posn ).asInstanceOf[ ShopItem ]

  // Running UI commands

  def doAdd = {
    val str = newItemText.getText.toString
    if ( str != "" ) {
      theList.addItem( description = str, isDone = false )
      newItemText.setText("")
    }
  }

  def doEdit( it: ShopItem ) = 
    editDialog.doEdit( it.description ) {
       theList.setItemDescription( it, _ )
    }

  def toggleDone( it: ShopItem ) = theList.setItemDone( it, !it.isDone )

  def deleteWhereDone = {
    if (theList.numDoneItems.value > 0) 
      theList.deleteWhereDone
    else
      toast( R.string.no_tasks_done )
  }

  def undelete = {
    if (theList.numDeletedItems.value > 0)
      theList.undeleteItems
    else
      toast( R.string.undeletes_exhausted )
  }
}

class ShopItemsAdapter( activity: PositronicActivity, 
                        query: ChangeNotifications[PositronicCursor] )
 extends CursorSourceAdapter( activity,
                              source = query,
                              converter = ShopItem.fromCursor(_),
                              itemViewResourceId = R.layout.item_row )
{
  def bindItem( view: View, it: ShopItem ) =
    view.asInstanceOf[ ShopItemView ].setShopItem( it )
}

// View for ShopItems:  adds strikethrough if the item "isDone" 

class ShopItemView( context: Context, attrs: AttributeSet = null )
  extends TextView( context, attrs ) 
{
   var theItem: ShopItem = null
   def getShopItem = theItem

   def setShopItem( item: ShopItem ) = {
     theItem = item
     setText( item.description )
     setPaintFlags( 
       if (item.isDone) getPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG 
       else getPaintFlags & ~Paint.STRIKE_THRU_TEXT_FLAG
     )
   }
}

// Getting sub-widgets, using the typed resources consed up by the
// android SBT plugin.  It would be nice to put this in a library,
// but the sbt-android plugin puts TypedResource itself in the app's
// main package --- so the library would have to import it from a
// different package in every app!

trait ViewFinder {
  def findView[T](  tr: TypedResource[T] ) = 
    findViewById( tr.id ).asInstanceOf[T]

  def findViewById( id: Int ): android.view.View
}


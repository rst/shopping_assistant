package org.positronicnet.demo.shopping

import org.positronicnet.ui.IndexedSeqAdapter
import org.positronicnet.ui.PositronicActivity
import org.positronicnet.ui.PositronicActivityHelpers
import org.positronicnet.ui.IndexedSeqSourceAdapter

import org.positronicnet.orm._
import org.positronicnet.orm.Actions._
import org.positronicnet.orm.SoftDeleteActions._

import org.positronicnet.notifications._
import org.positronicnet.notifications.Actions._

import android.content.Intent
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ContextMenu
import android.widget.SpinnerAdapter
import android.widget.TextView
import android.widget.Toast

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
    useAppFacility( ProxAlertManagement )
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
      ShoppingLists ! Save( ShoppingList( name = str ))
      findView( TR.newListName ).setText("")
    }
  }

  def doRename( list: ShoppingList ) =
    renameDialog.doEdit( list.name ){ 
      newName => ShoppingLists ! Save( list.setName( newName ))
    }

  def doDelete( list: ShoppingList ) = {
    ShoppingLists ! Delete( list )
    toast( R.string.list_deleted, Toast.LENGTH_LONG )
  }

  def doUndelete = { 
    ShoppingLists.hasDeleted ! Fetch{ hasDeleted => {
      if ( hasDeleted ) 
        ShoppingLists ! Undelete
      else 
        toast( R.string.undeletes_exhausted )
    }}
  }

  def viewListAt( posn: Int ) {
    val intent = new Intent( this, classOf[ ShoppingListActivity ] )
    val theList = listsView.getAdapter.getItem(posn).asInstanceOf[ShoppingList]
    intent.putExtra( "shopping_list_id", theList.id )
    startActivity( intent )
  }
}

// Adapter to wire up ShoppingList changes to the UI.
//
// Registers with the "source" (our ShoppingLists singleton) to be
// notified whenever its underlying data set is changed (or reloaded),
// so long as the activity is running.
//
// (MapActivities want to use this as well, so the constructor takes
// any activity with our ActivityHelpers trait.)

class ShoppingListsAdapter( activity: PositronicActivityHelpers )
  extends IndexedSeqSourceAdapter( activity, 
                                   source = ShoppingLists.records,
                                   itemViewResourceId=R.layout.shoppinglist_row)
  with SpinnerAdapter
{
  override def bindView( view: View, list: ShoppingList ) =
    view.asInstanceOf[ TextView ].setText( list.name )
}



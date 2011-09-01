package org.positronicnet.demo.shopping

import org.positronicnet.ui.PositronicActivity
import org.positronicnet.ui.IndexedSeqSourceAdapter

import org.positronicnet.orm._
import org.positronicnet.orm.Actions._
import org.positronicnet.orm.SoftDeleteActions._

import org.positronicnet.notifications._
import org.positronicnet.notifications.Actions._

import android.content.Intent
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.view.View
import android.view.KeyEvent
import android.view.ContextMenu
import android.util.AttributeSet
import android.widget.TextView

// The activity which manages an individual shopping list's items.

class ShoppingListActivity 
  extends PositronicActivity( layoutResourceId = R.layout.shopping_one_list ) 
  with ShoppingActivityCommonHelpers    // see UiMisc
{
  var theList: ShoppingList = null

  lazy val newItemText = findView( TR.newItemText )
  lazy val listItemsView = findView( TR.listItemsView )
  lazy val editDialog = new EditStringDialog( this )

  onCreate{

    useOptionsMenuResource( R.menu.items_view_menu )
    useContextMenuResource( R.menu.item_context_menu )

    // Setup --- get list out of our Intent, and hook up the listItemsView

    useAppFacility( ShoppingDb )
    useAppFacility( ProxAlertManagement )

    val listId = getIntent.getLongExtra( "shopping_list_id", -1 )

    ShoppingLists ! Find( listId, list => {
      theList = list
      setTitle( "Todo for: " + theList.name )
      listItemsView.setAdapter( new ShopItemsAdapter( this, 
                                                      theList.items.records ))
    })

    // Resynch prox alerts with current have-done-items state
    // when user quits.  

    onPause { 
      if ( theList != null ) ProxAlertManagement.resetProxAlerts( theList ) 
    }

    // Event handlers...

    listItemsView.onItemClick {( view, posn, id ) =>
      toggleDone( getDisplayedItem( posn )) }

    findView( TR.addButton ).onClick { doAdd }
    newItemText.onKey( KeyEvent.KEYCODE_ENTER ){ doAdd }

    installCommonOptionsMenuActions     // see UiMisc
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
      theList.items ! Save( theList.items.create.setDescription( str ))
      newItemText.setText("")
    }
  }

  def doEdit( it: ShopItem ) = 
    editDialog.doEdit( it.description ) { newDesc =>
      theList.items ! Save( it.setDescription( newDesc ))
    }

  def toggleDone( it: ShopItem ) = 
    theList.items ! Save( it.setDone( !it.isDone ))

  def deleteWhereDone = {
    theList.doneItems.count ! Fetch { 
      numDone =>
        if (numDone > 0)
          theList.doneItems ! DeleteAll
        else
          toast( R.string.no_tasks_done )
    }
  }

  def undelete = {
    theList.items.hasDeleted ! Fetch{ hasDeleted => {
      if ( hasDeleted )
        theList.items ! Undelete
      else
        toast( R.string.undeletes_exhausted )
    }}
  }
}

class ShopItemsAdapter( activity: PositronicActivity, 
                        query: Notifier[ IndexedSeq[ ShopItem ]] )
 extends IndexedSeqSourceAdapter( activity,
                                  source = query,
                                  itemViewResourceId = R.layout.item_row )
{
  override def bindView( view: View, it: ShopItem ) =
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


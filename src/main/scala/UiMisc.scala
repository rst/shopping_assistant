package org.positronicnet.demo.shopping

import org.positronicnet.ui.PositronicDialog
import org.positronicnet.ui.PositronicActivity
import org.positronicnet.ui.PositronicActivityHelpers

import android.content.Context
import android.content.Intent
import android.view.KeyEvent

// Various widgetry to deal with our available icons, and their display.

object ShoppingIcons {

  case class IconSet( smallResId: Int, largeResId: Int )

  val iconResIds =
    IndexedSeq( IconSet( R.drawable.bluecircle,  R.drawable.bluecirclebig  ),
                IconSet( R.drawable.redcircle,   R.drawable.redcirclebig   ),
                IconSet( R.drawable.greencircle, R.drawable.greencirclebig ))

  val numIcons = iconResIds.size

  def smallResId( list: ShoppingList ) = iconResIds( list.iconIdx ).smallResId
  def largeResId( list: ShoppingList ) = iconResIds( list.iconIdx ).largeResId

  def small( list: ShoppingList, ctx: Context ) = 
    ctx.getResources.getDrawable( iconResIds( list.iconIdx ).smallResId )

  def large( list: ShoppingList, ctx: Context ) = 
    ctx.getResources.getDrawable( iconResIds( list.iconIdx ).largeResId )
}

// Generic dialog box for editing a string.

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

// Common support code for our activities --- shorthand for dealing with
// the TypedResource stuff, and common menu actions

trait ShoppingActivityCommonHelpers 
  extends PositronicActivityHelpers
  with ViewFinder 
{
  def installCommonOptionsMenuActions = {
    onOptionsItemSelected( R.id.maps ) {
      startActivity( new Intent( this, classOf[ ShoppingMapActivity ] ))
    }
    onOptionsItemSelected( R.id.editlists ) {
      startActivity( new Intent( this, classOf[ ShoppingListsActivity ]) )
    }
  }
}

trait ViewFinder {
  def findView[T]( tr: TypedResource[T] ) = 
    findViewById( tr.id ).asInstanceOf[T]

  def findViewById( id: Int ): android.view.View
}

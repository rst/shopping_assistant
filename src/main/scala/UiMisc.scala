package org.positronicnet.demo.shopping

import org.positronicnet.ui.PositronicDialog
import org.positronicnet.ui.PositronicActivity

import android.content.Context
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

// Getting sub-widgets, using the typed resources consed up by the
// android SBT plugin.  It would be nice to put this in a library,
// but the sbt-android plugin puts TypedResource itself in the app's
// main package --- so the library would have to import it from a
// different package in every app!

trait ViewFinder {
  def findView[T]( tr: TypedResource[T] ) = 
    findViewById( tr.id ).asInstanceOf[T]

  def findViewById( id: Int ): android.view.View
}

package org.positronicnet.demo.shopping

import com.google.android.maps.MapActivity
import com.google.android.maps.MapView
import com.google.android.maps.ItemizedOverlay
import com.google.android.maps.OverlayItem
import com.google.android.maps.GeoPoint

import android.content.Context
import android.util.AttributeSet

import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

import android.graphics.drawable.Drawable

class DoubleTapMapView( context: Context, attrs: AttributeSet = null )
  extends MapView( context, attrs )
{
  val gestureDetector = 
    new GestureDetector( context,
                         new SimpleOnGestureListener {
                           override def onDoubleTap( ev: MotionEvent ):Boolean={
                             dispatchDoubleTap( ev )
                             return true
                           }
                         })

  private var doubleTapHandler: (GeoPoint => Unit) = null

  override def onTouchEvent( ev: MotionEvent ): Boolean = {
    if (gestureDetector.onTouchEvent( ev ))
      return true
    else
      return super.onTouchEvent( ev )
  }

  def onFreeDoubleTap( handler: GeoPoint => Unit ):Unit =
    doubleTapHandler = handler

  private def dispatchDoubleTap( ev: MotionEvent ): Unit = {

    // Dispatch a double tap to layers with an interest, or
    // our "free double tap handler", as appropriate.
    
    val x  = ev.getX.intValue   // Don't need sub-pixel precision here
    val y  = ev.getY.intValue
    val it = getOverlays.iterator

    while( it.hasNext ) {
      it.next match {
        case ov: DoubleTapDetection[_] =>
          ov.findIndexAtPosn( this, x, y ) match {
            case Some( idx ) => ov.onDoubleTap( idx ); return
            case None => // nothing
          }
        case _ => // nothing
      }
    }

    if (doubleTapHandler != null) {
      doubleTapHandler( getProjection.fromPixels( x, y ))
    }
  }
}

// Supporting trait for our itemized overlays.
//
// Needs to be able to get the drawable we're using for the items
// (I *think* --- the docs are less than explicit), so our shim
// Java classes have been gimmicked to provide that.

trait DoubleTapDetection[T <: OverlayItem] extends ItemizedOverlay[T] {

  // Note the public variant of hitTest declared in our Java shim
  // ItemizedOverlay class.  (It can't be protected due to a Scala
  // "implementation restriction"...).
  //
  // Also note the odd coordinate gymnastics needed to actually use
  // hitTest.  The documentation here leaves a great deal to be desired;
  // this code is cargo-culted out of an email thread here:
  //
  // http://groups.google.com/group/android-developers/browse_thread/thread/b4484fbc1ec00c1d

  def hitTest( it: T, x: Int, y: Int ): Boolean

  def findIndexAtPosn( mapView: MapView, x: Int, y: Int ) = {
    Range( 0, size ).find{ idx => 
      val item = getItem( idx )
      val pt = mapView.getProjection.toPixels( item.getPoint, null )
      hitTest( item, x - pt.x, y - pt.y )
    }
  }

  var doubleTapHandler: (Int => Unit) = null

  def onDoubleTap( func: Int => Unit ) = { doubleTapHandler = func }

  def onDoubleTap( idx: Int ):Unit = 
    if (doubleTapHandler != null) doubleTapHandler( idx )
}

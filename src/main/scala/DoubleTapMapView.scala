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

  def onDoubleTap( handler: GeoPoint => Unit ):Unit =
    doubleTapHandler = handler

  private def dispatchDoubleTap( ev: MotionEvent ): Unit = {

    // If this double tap is at a point near the items for any of our
    // itemizedOverlays, that overlay's onTap has probably already
    // handled it.  And in any case, there's an item there, which means
    // that our intended double-tap handling --- adding another item
    // --- is probably not what we want here anyway.  So, suppress it.
    
    val x  = ev.getX.intValue   // Don't need sub-pixel precision here
    val y  = ev.getY.intValue
    val it = getOverlays.iterator

    while( it.hasNext ) {
      it.next match {
        case ov: DoubleTapDetection[_] =>
          if (ov.isItemAtPosn( x, y )) return
        case _ => null
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
  // "implementation restriction"...)

  def hitTest( it: T, x: Int, y: Int ): Boolean

  private def findIndexAtPosn( x: Int, y: Int ) =
    Range( 0, size ).find{ idx => hitTest( getItem( idx ), x, y )}

  def findItemAtPosn( x: Int, y: Int ) =
    findIndexAtPosn( x, y ).map{ getItem(_) }

  def isItemAtPosn( x: Int, y: Int ) = 
    findIndexAtPosn( x, y ) != None
}

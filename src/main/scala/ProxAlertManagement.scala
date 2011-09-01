package org.positronicnet.demo.shopping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent

import android.location.LocationManager
import android.util.Log

// All prox alert handling wrapped up in the ProxAlertManagement class
// and companion object (an AppFacility which sets up the alerts),
// which between them know the structure of the intents we're
// sending...

object ProxAlertManagement 
  extends org.positronicnet.util.AppFacility
{
  val shopIdKey = "shop_id"
  val listIdKey = "list_id"

  val alertRadiusMeters = 300           // Should be a pref...

  private var locManager: LocationManager = null
  private var ctx:        Context         = null

  override def realOpen( ctx: Context ) = {
    this.ctx = ctx.getApplicationContext
    locManager = ctx.getSystemService( Context.LOCATION_SERVICE )
      .asInstanceOf[ LocationManager ]
  }

  override def realClose = {
    this.ctx = null
    this.locManager = null
  }

  // NB we set a prox alert for each shop, sticking the shop ID in the
  // extras to hopefully keep the alerts for shops on the same list from
  // canceling each other out.  

  def resetAllProxAlerts: Unit =
    for ( list <- ShoppingLists.fetchOnThisThread;
          shop <- list.shops.fetchOnThisThread )
      resetProxAlert( shop, list )

  def resetProxAlert( shop: Shop ): Unit =
    resetProxAlert( shop, shop.shoppingList.fetchOnThisThread )

  def resetProxAlert( shop: Shop, list: ShoppingList ): Unit = {
    Log.d( "XXX", "adding prox alert for shop " + shop.id + " at lat " +
          (shop.latitude / 1e6).toString + " long " + 
          (shop.longitude / 1e6).toString
          )
    locManager.addProximityAlert( shop.latitude / 1e6,
                                  shop.longitude / 1e6,
                                  ProxAlertManagement.alertRadiusMeters,
                                  -1,
                                  buildPendingIntent( list, shop ))
  }

  def deleteProxAlert( shop: Shop ): Unit =
    deleteProxAlert( shop, shop.shoppingList.fetchOnThisThread )

  def deleteProxAlert( shop: Shop, list: ShoppingList ): Unit = {
    Log.d( "XXX", "removing prox alert for shop " + shop.id + " at lat " +
          (shop.latitude / 1e6).toString + " long " + 
          (shop.longitude / 1e6).toString
          )
    locManager.removeProximityAlert( buildPendingIntent( list, shop ))
  }

  def buildPendingIntent( list: ShoppingList, shop: Shop ) = {
    val intent = new Intent( ctx, classOf[ ProxAlertManagement ] )
    intent.putExtra( shopIdKey, shop.id )
    intent.putExtra( listIdKey, shop.shoppingListId )
    PendingIntent.getBroadcast( ctx, 0, intent,
                                PendingIntent.FLAG_CANCEL_CURRENT )
  }
}

class ProxAlertManagement
  extends BroadcastReceiver
{
  // We use list ID as key of the notifications --- at most one alert
  // per list.

  override def onReceive( ctx: Context, intent: Intent ) : Unit = {

    ShoppingDb.openInContext( ctx )
    ProxAlertManagement.openInContext( ctx )

    try {
      val notificationMgr = ctx.getSystemService( Context.NOTIFICATION_SERVICE )
        .asInstanceOf[ NotificationManager ]

      val listId   = intent.getLongExtra( ProxAlertManagement.listIdKey, -1 )
      val list     = ShoppingLists.findOnThisThread( listId )
      val entering = 
        intent.getBooleanExtra( LocationManager.KEY_PROXIMITY_ENTERING, false )

      if (!entering) {
        Log.d( "XXX", "leaving " + list.name)
        notificationMgr.cancel( listId.toInt )
      }
      else {
        Log.d( "XXX", "entering " + list.name )

        val n = new Notification( ShoppingIcons.smallResId( list ),
                                  "Near a " + list.name + "; go shop!",
                                  System.currentTimeMillis )

        val viewIntent = new Intent( ctx, classOf[ ShoppingListActivity ])
        viewIntent.putExtra( "shopping_list_id", list.id )

        n.setLatestEventInfo( ctx, "Time to go shopping",
                              "You're near a " + list.name + 
                              " where you have pending items",
                              PendingIntent.getActivity( ctx, 0, viewIntent, 0))

        notificationMgr.notify( list.id.toInt, n )
      }
    }
    finally {
      ShoppingDb.close
      ProxAlertManagement.close
    }
  }
}

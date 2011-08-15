package org.positronicnet.demo.shopping

import android.util.Log
import android.content.Intent

import org.positronicnet.db.Database
import org.positronicnet.db.DbQuery
import org.positronicnet.content.PositronicCursor

import org.positronicnet.util.WorkerThread
import org.positronicnet.util.ChangeManager

import scala.collection.mutable.ArrayBuffer

// Our domain model classes, such as they are:  Shops, Shopping Lists, etc.
// 
// There's no ORM here, just an AREL-style gloss for building SQL, and
// a stylized way of building model classes that use it.  But it still
// cuts down on the clutter.
//
// NB operations on these affect the database, so they happen on a
// separate "db thread".  UI components can register as listeners for
// changes on domain objects, and if they do, they get fresh cursors
// with which to update themselves when things do change.  
//
// Note also that we're managing a "soft deletion" scheme here.
// User-level "delete" operations just set an "is_deleted" flag on the
// objects of the user's disaffection; they don't actually delete them
// immediately.  If the user has second thoughts, they can then
// "undelete" (resetting the flag) until the next batch of deletions,
// at which point the last batch really is purged.  This tends to be
// more effective than confirmation dialogs at helping users recover
// from mistakes.

// Start by defining the DB schema...

object ShoppingDb 
 extends Database( filename = "shopping.sqlite3", logTag = "shopping" ) 
 with WorkerThread
{
  // This gets fed to a SQLiteOpenHelper, which implements the following
  // default behavior (unless overridden, of course):
  //
  // "version" is the length of schemaUpdates.
  // "onUpdate" runs all the updates from oldVersion to newVersion.
  // "onCreate" just runs 'em all.

  def schemaUpdates =
    List(""" create table shopping_lists (
               _id integer primary key,
               name string,
               is_deleted integer default 0,
               icon_idx integer default 0
             )
         """,
         """ create table shop_items (
               _id integer primary key,
               shopping_list_id integer,
               description string,
               is_done integer,
               is_deleted integer default 0
             )
         """,
         """ create table shops (
               _id          integer primary key,
               shopping_list_id integer,
               latitude     integer,
               longitude    integer,
               description  string,
               is_deleted   integer default 0
             )
         """
        )
  
}

//================================================================
// "Shop item" model.
// 
// Mostly actually manipulated from within ShoppingList; with a more
// complicated schema, it might be better to get these query fragments
// from methods invoked on the ShopItem companion object.

case class ShopItem(var id: Long, var description: String, var isDone: Boolean)

object ShopItem {
  def seqFromRows( query: DbQuery ) = 
    query.select("_id", "description", "is_done").map {
      c => ShopItem( c.getLong( 0 ), c.getString( 1 ), c.getBoolean( 2 ))
    }
}

//================================================================
// "Shop" model.
//
// Places associated with a particular shopping list.
// Latitude and longitude are as per Map UI standard rep
// (degrees * 1e6, truncated to integer).

case class Shop( var id: Long, var description: String,
                      var latitude: Int, var longitude: Int )

object Shop {
  def seqFromRows( query: DbQuery ) = 
    query.select("_id", "description", "latitude", "longitude").map {
      c => Shop( c.getLong( 0 ), c.getString( 1 ), 
                 c.getInt( 2 ),  c.getInt( 3 ) )
    }
}

//================================================================
// "Shopping list" model.  
// Includes most actual manipulation of items and shops.

case class ShoppingList( var id: Long, var name: String, var iconIdx: Int )
 extends ChangeManager( ShoppingDb )
{
  // Setting up (and use of) prebaked query fragments.

  private lazy val dbItemsAll = 
    ShoppingDb( "shop_items" ).whereEq( "shopping_list_id" -> id )
  private lazy val dbItems = 
    dbItemsAll.whereEq( "is_deleted" -> false )

  // Things that UI elements (etc.) can monitor

  lazy val items = valueStream { ShopItem.seqFromRows( dbItems ) }

  def itemsQuery( initialShowDone: Boolean ) = {
    valueQuery( initialShowDone ){ showDone => 
      ShopItem.seqFromRows(
        if ( showDone ) dbItems else dbItems.whereEq( "is_done" -> false )
      )
    }
  }

  lazy val numUndoneItems = valueStream {
    dbItems.whereEq( "is_done" -> false ).count 
  }
  lazy val numDoneItems = valueStream { 
    dbItems.whereEq( "is_done" -> true ).count 
  }
  lazy val numDeletedItems= valueStream { 
    dbItemsAll.whereEq( "is_deleted" -> true).count
  }

  // Changes that UI elements (etc.) can ask for

  def addItem( description: String, isDone: Boolean = false ) = doChange { 
    ShoppingDb( "shop_items" ).insert( 
        "shopping_list_id" -> this.id, 
        "description"      -> description,
        "is_done"          -> isDone )
  }

  def setItemDescription( it: ShopItem, desc: String ) = doChange { 
    dbItems.whereEq("_id" -> it.id).update( "description" -> desc )
  }

  def setItemDone( it: ShopItem, isDone: Boolean ) = doChange { 
    dbItems.whereEq("_id" -> it.id).update( "is_done" -> isDone )
  }

  def deleteWhereDone = doChange {
    dbItemsAll.whereEq( "is_deleted" -> true ).delete // purge the last batch
    dbItems.whereEq( "is_done" -> true ).update( "is_deleted" -> true )
  }

  def undeleteItems = doChange { dbItemsAll.update( "is_deleted" -> false ) }

  // Likewise for shops, more or less... 

  private lazy val dbShopsAll = 
    ShoppingDb( "shops" ).whereEq( "shopping_list_id" -> id )
  private lazy val dbShops = 
    dbShopsAll.whereEq( "is_deleted" -> false )

  lazy val places = valueStream { Shop.seqFromRows( dbShops ) }
  lazy val hasDeletedPlace = valueStream {
    dbShopsAll.whereEq( "is_deleted" -> true ).count > 0
  }

  def addPlace( latitude: Int, longitude: Int ) = doChange {
    ShoppingDb( "shops" ).insert(
      "shopping_list_id" -> this.id,
      "latitude"         -> latitude,
      "longitude"        -> longitude )
  }
    
  def setPlaceDescription( place: Shop, desc: String ) = doChange {
    dbShops.whereEq("_id" -> place.id).update( "description" -> desc )
  }

  def deletePlace( place: Shop ) = doChange {
    ProxAlertManagement.deleteProxAlert( this, place )
    dbShopsAll.whereEq( "is_deleted" -> true ).delete
    dbShopsAll.whereEq( "_id" -> place.id ).update( "is_deleted" -> true )
  }

  def undeletePlace = doChange {
    dbShopsAll.update( "is_deleted" -> false )
  }

  // And prox alert updates.  Maximum crudeness:  we clobber all prox alerts
  // for the list on any change whatever.  What's tricky about doing better
  // here is that with stuff written as above, "the shop that changed" is
  // not available as an object until after it's read out of the DB.
  // Hopefully, with an ORM, we can do better...
  //
  // At least this is happening on a background thread!

  val forceProxAlertUpdates = valueStream{
    ProxAlertManagement.resetAllProxAlerts( this )
  }
}

object ShoppingList {

  def seqFromRows( query: DbQuery ) = 
    query.select("_id", "name", "icon_idx").map {
      c => ShoppingList( c.getLong( 0 ), c.getString( 1 ), c.getInt( 2 ))
    }

  def create( name: String ) = ShoppingDb("shopping_lists").insert("name"->name)

  // Communicating these through intents...
  // Sadly, this is easier than making them serializable.

  val intentIdKey   = "ShoppingListId"; 
  val intentNameKey = "ShoppingListName"
  val intentIconKey = "ShoppingListIcon"

  def intoIntent( list: ShoppingList, intent: Intent ) = {
    intent.putExtra( intentIdKey,   list.id )
    intent.putExtra( intentNameKey, list.name )
    intent.putExtra( intentIconKey, list.iconIdx )
  }

  def fromIntent( intent: Intent ) = 
    ShoppingList( intent.getLongExtra( intentIdKey, -1 ), 
              intent.getStringExtra( intentNameKey ),
              intent.getIntExtra( intentIconKey, -1 )
            )
}

//================================================================
// Singleton object to represent the set of all available lists.

object ShoppingLists extends ChangeManager( ShoppingDb )
{
  private lazy val dbListsAll = ShoppingDb("shopping_lists")
  private lazy val dbLists = dbListsAll.whereEq("is_deleted"-> false)

  // Things UI can monitor

  lazy val lists = valueStream { ShoppingList.seqFromRows( dbLists ) }

  lazy val numDeletedLists= valueStream {
    dbListsAll.whereEq("is_deleted"-> true).count
  }

  // Changes UI can request

  def addList( name: String ) = doChange { ShoppingList.create( name ) }

  def setListName( list: ShoppingList, newName: String ) = doChange {
    dbLists.whereEq("_id" -> list.id).update( "name" -> newName )
  }

  def setListIconIdx( list: ShoppingList, newIdx: Int ) = doChange {
    list.iconIdx = newIdx               // Yeah, an ORM would be nice here...
    dbLists.whereEq("_id" -> list.id).update( "icon_idx" -> newIdx )
  }

  def removeList( victim: ShoppingList ) = doChange {

    // Purge all previously deleted lists...
    for ( c <- dbListsAll.whereEq( "is_deleted" -> true ).select( "_id" )) {
      val purgedListId = c.getLong(0)
      ShoppingDb("shops").whereEq("shopping_list_id"->purgedListId).delete
      ShoppingDb("shop_items").whereEq("shopping_list_id"->purgedListId).delete
      ShoppingDb("shopping_lists").whereEq( "_id" -> purgedListId ).delete
    }

    // And mark this one for the axe...
    dbListsAll.whereEq( "_id" -> victim.id ).update( "is_deleted" -> true )
  }

  def undelete = doChange { dbListsAll.update( "is_deleted" -> false ) }
} 


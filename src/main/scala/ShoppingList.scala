package org.positronicnet.demo.shopping

import org.positronicnet.db.Database
import org.positronicnet.content.ContentQuery
import org.positronicnet.orm._
import org.positronicnet.orm.Actions._

import org.positronicnet.util.WorkerThread

// Our domain model classes, such as they are:  Shops, Shopping Lists, etc.
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

         // "description" and "is_deleted" on shops not used yet...

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

// "Shopping list" model.  

case class ShoppingList( name:    String = null,
                         iconIdx: Int    = 0,
                         id:      Long   = ManagedRecord.unsavedId
                       )
  extends ManagedRecord( ShoppingLists )
{
  def setName( s: String ) = copy( name    = s )
  def setIconIdx( i: Int ) = copy( iconIdx = i )

  lazy val items = new HasMany( ShopItems ) with SoftDeleteQueries[ ShopItem ] 
  lazy val shops = new HasMany( Shops )

  lazy val doneItems   = items.whereEq( "is_done" -> false )
  lazy val undoneItems = items.whereEq( "is_done" -> true  )
}

object ShoppingLists extends RecordManager[ ShoppingList ]( 
                                            ShoppingDb("shopping_lists") )
  with SoftDelete[ ShoppingList ]

// "Shop item" model.

case class ShopItem( shoppingListId: Long = ManagedRecord.unsavedId,
                     description: String  = null, 
                     isDone: Boolean      = false,
                     id: Long             = ManagedRecord.unsavedId 
                   )
  extends ManagedRecord( ShopItems )
{
  def setDescription( s: String ) = copy( description = s )
  def setDone( b: Boolean )       = copy( isDone = b )
}

object ShopItems extends RecordManager[ ShopItem ]( ShoppingDb( "shop_items" ))
  with SoftDelete[ ShopItem ]

// "Shop" model.
//
// Places associated with a particular shopping list.
// Latitude and longitude are as per Map UI standard rep
// (degrees * 1e6, truncated to integer).
//
// Note that the Shops RecordManager is responsible for
// managing interaction with the proximity alert manager,
// as and when needed.

case class Shop( shoppingListId: Long = ManagedRecord.unsavedId,
                 latitude: Int        = 0,
                 longitude: Int       = 0,
                 id: Long             = ManagedRecord.unsavedId 
               )
  extends ManagedRecord( Shops )
{
  def setLocation( lat: Int, long: Int ) = 
    copy( latitude = lat, longitude = long )

  lazy val shoppingList = new BelongsTo( ShoppingLists )
}

object Shops extends RecordManager[ Shop ]( ShoppingDb( "shops" ))
  with ParentSoftDeleteListener[ ShoppingList ]
{
  override def save( shop: Shop, scope: Scope[Shop] ) = {
    // XXX need the ID!
    super.save( shop, scope )
    ProxAlertManagement.resetProxAlert( shop )
  }

  override def deleteAll( qry: ContentQuery[_,_], scope: Scope[Shop] ) = {

    for ( shop <- fetchRecords( qry ))
      ProxAlertManagement.deleteProxAlert( shop )

    super.deleteAll( qry, scope )
  }

  // If a ShoppingList has been soft-deleted, its Shops really
  // shouldn't have active prox alerts (both because the user isn't
  // expecting them, and because we don't want the prox alert
  // machinery taking unnecessary fine position fixes nearby).

  def onParentSoftDelete(qry: ContentQuery[_,_], scope: Scope[ ShoppingList ])={
    for( list <- ShoppingLists.fetchRecords( qry );
         shop <- list.shops.fetchOnThisThread )
      ProxAlertManagement.deleteProxAlert( shop, list )
  }

  def onParentUndelete(qry: ContentQuery[_,_], scope: Scope[ ShoppingList ]) = {
    for( list <- ShoppingLists.fetchRecords( qry );
         shop <- list.shops.fetchOnThisThread )
      ProxAlertManagement.resetProxAlert( shop, list )
  }
}

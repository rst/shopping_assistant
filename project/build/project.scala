import sbt._

trait Defaults {
  def androidPlatformName = "android-7"
}
class Parent(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val shopping = project(".", "shoppinglists", new SampleProject(_))

  class SampleProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish with TypedResources {
    val keyalias  = "change-me"
    val positroniclib = "org.positronicnet" %% "positronicnetlib" % "0.1"
  }
}

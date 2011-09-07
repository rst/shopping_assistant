import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    version := "0.1",
    scalaVersion := "2.9.0-1",
    platformName in Android := "android-7"
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me",
      libraryDependencies ++= Seq(
        "org.scalatest"     %% "scalatest"        % "1.6.1"        % "test",
        "org.positronicnet" %% "positronicnetlib" % "0.3-SNAPSHOT"
      ),
      // Next bit looks awkward here, but this is where it works...
      unmanagedSourceDirectories in Compile <+= 
        baseDirectory(_/"mapviewballoons/src/main/java"))
}

object AndroidBuild extends Build {
  lazy val proj = Project (
    "PositronicShoppingLists",
    file("."),
    settings = General.fullAndroidSettings
  )
}

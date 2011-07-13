import sbt._
import Process._

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

    // We want to keep the source for the "mapviewballoons" android
    // library project separate from those for the app proper.
    // Instead, we arrange to pick up the source code and resources
    // from its segregated directory here, and give it a gimmicked
    // R.java that copies resource IDs from the one that aapt
    // generates for the app itself.  It's a kludge, but I'd rather do
    // this than comingle my resources with the ones from the library
    // project.

    val androidLibProjectDirs = Array( "mapviewballoons" )

    override def mainSourceRoots = 
      androidLibProjectDirs.foldLeft( super.mainSourceRoots ) { 
        (paths, dir) => paths +++ ( dir / "src" / "main" / "java" ) }

    val androidLibResourceDirs =
      androidLibProjectDirs.map{ _/"src"/"main"/"res" }

    // Redo aapt packaging to pick up resources from the pseudo-android 
    // lib project.  (Pseudo because I punted, and generated an R.java
    // there by hand which references the app's own.)

    override def aaptGenerateTask = execTask {<x>
      {aaptPath.absolutePath} package -m -M {androidManifestPath.absolutePath} -S {mainResPath.absolutePath}
         --auto-add-overlay
         {androidLibResourceDirs.map{ " -S " + _.absolutePath }}
         -I {androidJarPath.absolutePath} -J {mainJavaSourcePath.absolutePath}
    </x>} dependsOn directory(mainJavaSourcePath)

    override def aaptPackageTask = execTask {<x>
      {aaptPath.absolutePath} package -f -M {androidManifestPath.absolutePath} -S {mainResPath.absolutePath} 
         --auto-add-overlay
         {androidLibResourceDirs.map{ " -S " + _.absolutePath }}
         -A {mainAssetsPath.absolutePath} -I {androidJarPath.absolutePath} -F {resourcesApkPath.absolutePath}
       </x>} dependsOn directory(mainAssetsPath)
  }
}

// library projects
//
// class LibProject(info: ProjectInfo) extends AndroidProject(info) with Defaults}
//
//   lazy val mapviewballoons = project("mapviewballoons", "mapviewballoons",
//                                     new LibProject(_))
//    // Redo aapt packaging to pick up resources from AndroidProject
//    // dependencies
//
//    override def aaptPackageTask = execTask {<x>
//      {aaptPath.absolutePath} package -f -M {androidManifestPath.absolutePath} -S {mainResPath.absolutePath} 
//         {dependencies.map(_ match {
//           case ap: AndroidProject => " -S " + ap.mainResPath.absolutePath
//           case _ => ""
//         })}
//         -A {mainAssetsPath.absolutePath} -I {androidJarPath.absolutePath} -F {resourcesApkPath.absolutePath}
//       </x>} dependsOn directory(mainAssetsPath)
//
//
//    // redo packageTask to pick up resource APKs from library subprojects
//  
//    override def packageTask(signPackage: Boolean) = execTask {<x>
//      {apkbuilderPath.absolutePath}  {packageApkPath.absolutePath}
//        {if (signPackage) "" else "-u"} -z {resourcesApkPath.absolutePath} -f {classesDexPath.absolutePath}
//        {proguardInJars.get.map(" -rj " + _.absolutePath)}
//        {dependencies.map(_ match {
//          case ap: LibProject => " -z " + ap.resourcesApkPath.absolutePath
//          case _ => "" })}
//    </x>} dependsOn(cleanApk)

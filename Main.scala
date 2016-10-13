import java.io._
import java.nio.file._
import cbt._
import xsbti._

object Main{
  def main(args: Array[String]): Unit = {
    //System.setProperty("sbt.boot.properties",System.getProperty("user.dir")+"/sbt.boot.properties")
    //val cl = .head.classLoader( context.classLoaderCache )
    new sbt.xMain().run(
      new AppConfiguration{
        def arguments = args.toArray
        def baseDirectory = new File(System.getProperty("user.dir"))
        def provider = new AppProvider{
          /** Returns the ScalaProvider that this AppProvider will use. */
          def scalaProvider = new CbtScalaProvider("2.10.6")
          /** The ID of the application that will be created by 'newMain' or 'mainClass'.*/
          def id = new ApplicationID{
            /**
             * @return
             *   The Ivy orgnaization / Maven groupId where we can find the application to launch.
             */
            def groupID: String = ???
            /**
             * @return
             *    The ivy module name / Maven artifactId where we can find the application to launch.
             */
            def name: String = ???
            /**
             * @return
             *    The ivy/maven version of the module we should resolve.
             */
            def version: String = "dummyVersion"

            /**
             * @return
             *    The fully qualified name of the class that extends xsbti.AppMain
             */
            def mainClass: String = ???
            /**
             * @return
             *    Additional ivy components we should resolve with the main application artifacts.
             */
            def mainComponents: Array[String] = ???
            /**
             * @deprecated
             * This method is no longer used if the crossVersionedValue method is available.
             * 
             * @return
             *    True if the application is cross-versioned by binary-compatible version string,
             *    False if there is no cross-versioning.
             */
            def crossVersioned: Boolean = ???
            
            /**
             * 
             * @since 0.13.0
             * @return
             *    The type of cross-versioning the launcher should use to resolve this artifact.
             */
            def crossVersionedValue: CrossValue = ???
            
            /** Files to add to the application classpath. */
            def classpathExtra: Array[File] = ???
          }
          /** The classloader used to load this application. */
          def loader: ClassLoader = ???
          /** Loads the class for the entry point for the application given by 'id'.  
           * This method will return the same class every invocation.  
           * That is, the ClassLoader is not recreated each call.
           * @deprecated("use entryPoint instead")
           * 
           * Note:  This will throw an exception if the launched application does not extend AppMain.
           */
          def mainClass: Class[_ <: AppMain] = ???
          /** Loads the class for the entry point for the application given by 'id'.  
           * This method will return the same class every invocation.  
           * That is, the ClassLoader is not recreated each call.
           */
          def entryPoint: Class[_] = ???
          /** Creates a new instance of the entry point of the application given by 'id'.
          * It is NOT guaranteed that newMain().getClass() == mainClass().
          * The sbt launcher can wrap generic static main methods. In this case, there will be a wrapper class,
          * and you must use the `entryPoint` method.
          * @throws IncompatibleClassChangeError if the configuration used for this Application does not
          *                                         represent a launched application.
          */
          def newMain: AppMain = ???
          
          /** The classpath from which the main class is loaded, excluding Scala jars.*/
          def mainClasspath: Array[File] = Array() // FIXME

          /** Returns a mechanism you can use to install/find/resolve components.  
           * A component is just a related group of files.
           */
          def components: ComponentProvider = ???
        }
      }
    )
    //lib.runMain("xsbt.boot.Boot",/*Seq("shell") ++ */context.args, cl, trapExitCode = false)
  }
}

class CbtScalaProvider(version: String, scalaOrg: String = "org.scala-lang") extends ScalaProvider{ scalaProvider =>
  def launcher = new Launcher{
    def getScala(version: String): ScalaProvider = new CbtScalaProvider(version)
    def getScala(version: String, reason: String): ScalaProvider = new CbtScalaProvider(version)
    def getScala(version: String, reason: String, scalaOrg: String): ScalaProvider = new CbtScalaProvider(version, scalaOrg)
    /**
     * returns an `AppProvider` which is able to resolve an application
     * and instantiate its `xsbti.Main` in a new classloader.
     * See [AppProvider] for more details.
     * @param id  The artifact coordinates of the application.
     * @param version The version to resolve
     */
    def app(id: ApplicationID, version: String): AppProvider = ???
    /**
     * This returns the "top" classloader for a launched application.   This classlaoder
     * lives somewhere *above* that used for the application.   This classloader
     * is used for doing any sort of JNA/native library loads so that downstream
     * loaders can share native libraries rather than run into "load-once" restrictions.
     */
    def topLoader: ClassLoader = getClass.getClassLoader
    /**
     * Return the global lock for interacting with the file system.
     *
     * A mechanism to do file-based locking correctly on the JVM.  See
     * the [[GlobalLock]] class for more details.
     */
    def globalLock: GlobalLock = ???
    /** Value of the `sbt.boot.dir` property, or the default
     *  boot configuration defined in `boot.directory`.
     */
    def bootDirectory: File = ???
    /** Configured launcher repositories.  These repositories
     *  are the same ones used to load the launcher.
     */
    def ivyRepositories: Array[xsbti.Repository] = ???
    /** These are the repositories configured by this launcher
     * which should be used by the application when resolving
     * further artifacts.
     */
    def appRepositories: Array[xsbti.Repository] = ???
    /** The user has configured the launcher with the only repositories
     * it wants to use for this applciation.
     */
    def isOverrideRepositories: Boolean = ???
    /**
     * The value of `ivy.ivy-home` of the boot properties file.
     * This defaults to the `sbt.ivy.home` property, or `~/.ivy2`.
     * Use this setting in an application when using Ivy to resolve
     * more artifacts.
     *
     * @returns a file, or null if not set.
     */
    def ivyHome: File = new File(System.getProperty("user.home")+"/.ivy2").getAbsoluteFile
    /** An array of the checksums that should be checked when retreiving artifacts.
     *  Configured via the the `ivy.checksums` section of the boot configuration.
     *  Defaults to sha1, md5 or the value of the `sbt.checksums` property.
     */
    def checksums: Array[String] = ???
  }
  /** The version of Scala this instance provides.*/
  def version: String = scalaProvider.version

  implicit private lazy val logger = new cbt.Logger(Set[String](),System.currentTimeMillis)
  private lazy val dependency = BoundMavenDependency(
    false,
    BuildInfo.mavenCache,
    MavenDependency(scalaOrg, "scala-compiler", version),
    Seq(mavenCentral)
  )(logger)
  
  private lazy val classLoaderCache = new ClassLoaderCache( logger, new java.util.concurrent.ConcurrentHashMap, new java.util.concurrent.ConcurrentHashMap )
  
  /** A ClassLoader that loads the classes from scala-library.jar and scala-compiler.jar.*/
  def loader: ClassLoader = ??? /*Dependencies( Seq(
    dependency,
    BoundMavenDependency(
      false,
      BuildInfo.mavenCache,
      MavenDependency("org.scala-sbt", "zinc-compile-core_2.11","1.0.0-X4"),
      Seq(mavenCentral)
    )(logger)
  )).classLoader(classLoaderCache) // FIXME: use cbt's for speedup
  */
  /** Returns the scala-library.jar and scala-compiler.jar for this version of Scala. */
  def jars: Array[File] = {
    val paths = dependency.classpath.strings.map(Paths.get(_))
    val targets = paths.map(_.getFileName.toString.replace("-"+version,"")).map(Paths.get(_)).map(_.toAbsolutePath)
    println(targets);
    (paths zip targets).foreach{
      case (s, t) =>
      Files.copy(s,t, StandardCopyOption.REPLACE_EXISTING)
    }
    targets.map(_.toFile).toArray
  }

  /**@deprecated Only `jars` can be reliably provided for modularized Scala. (Since 0.13.0) */
  def libraryJar: File = ???

  /**@deprecated Only `jars` can be reliably provided for modularized Scala. (Since 0.13.0) */
  def compilerJar: File = ???

  /** Creates an application provider that will use 'loader()' as the parent ClassLoader for
  * the application given by 'id'.  This method will retrieve the application if it has not already
  * been retrieved.*/
  def app(id: ApplicationID): AppProvider = ???
}
import cbt._
import java.nio._
import java.nio.file._

class Build(val context: Context) extends BaseBuild{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Seq(
      context.cbtDependency
      // source dependency
      // DirectoryDependency( projectDirectory ++ "/subProject" )
    ) ++
    // pick resolvers explicitly for individual dependencies (and their transitive dependencies)
    Resolver( mavenCentral, sonatypeReleases ).bind(
      // CBT-style Scala dependencies
      // ScalaDependency( "com.lihaoyi", "ammonite-ops", "0.5.5" )
      // MavenDependency( "com.lihaoyi", "ammonite-ops_2.11", "0.5.5" )

      // SBT-style dependencies
      // "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
      // "com.lihaoyi" % "ammonite-ops_2.11" % "0.5.5"
      ScalaDependency(
        "org.scala-sbt", "main", "1.0.0-M4"
      )
    )

    override def compile = {
      Files.write(
        (projectDirectory ++ "/BuildInfo.scala").toPath,
s"""
import java.io._
object BuildInfo{
  def mavenCache = new File("${context.paths.mavenCache}")
}
""".getBytes
      )
      super.compile
    }
    // how sbt determines current directory:
    // (new File("")).getAbsoluteFile
    // it then sets
    // System.getProperty("user.dir")

}

//exec java -Xms512m -Xmx1536m -Xss2m -XX:ReservedCodeCacheSize=256m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -Xms512M -Xmx1536M -Xss4M -XX:+CMSClassUnloadingEnabled -XX:ReservedCodeCacheSize=256M -jar 

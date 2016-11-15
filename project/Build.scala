import sbt.Keys._
import sbt._

object Utils extends Build {

  val branch = Process("git" :: "rev-parse" :: "--abbrev-ref" :: "HEAD" :: Nil).!!.trim
  val suffix = "-SNAPSHOT" // if (branch == "master") "" else "-SNAPSHOT" //Dev phase now! Pending release.

  val libVersion = "0.0.6" + suffix


  def scalacOptionsVersion(sv: String): Seq[String] = {
    Seq(
      // Note: Add -deprecation when deprecated methods are removed
      "-unchecked",
      "-feature",
      "-encoding", "utf8"
    ) ++ (CrossVersion.partialVersion(sv) match {
      // Needs -missing-interpolator due to https://issues.scala-lang.org/browse/SI-8761
      case Some((2, x)) if x >= 11 => Seq("-Xlint:-missing-interpolator")
      case _ => Seq("-Xlint")
    })
  }

  val sharedSettings = Seq(
    version := libVersion,
    organization := "com.flipkart",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    // Workaround for a scaladoc bug which causes it to choke on empty classpaths.
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")),
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.8.1" % "test",
      "org.mockito" % "mockito-all" % "1.9.5" % "test",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    ),

    scalacOptions := scalacOptionsVersion(scalaVersion.value),

    // Note: Use -Xlint rather than -Xlint:unchecked when TestThriftStructure
    // warnings are resolved
    javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-target", "1.7"),
    javacOptions in doc := Seq("-source", "1.7"),

    // This is bad news for things like com.twitter.util.Time
    parallelExecution in Test := false,

    // Sonatype publishing
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    autoAPIMappings := true,
    apiURL := Some(url("https://github.com/Flipkart/utils")),
    pomExtra :=
      <url>https://github.com/Flipkart/utils</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:Flipkart/utils</url>
          <connection>scm:git:git@github.com:Flipkart/utils</connection>
        </scm>
        <developers>
          <developer>
            <id>Flipkart</id>
            <name>Flipkart Internet Pvt. Ltd.</name>
            <url>https://www.flipkart.com/</url>
          </developer>
        </developers>
  )


  lazy val util = Project(
    id = "utils",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ) aggregate(
    utilCore, utilConfig, utilHttp
    )

  lazy val utilCore = Project(
    id = "util-core",
    base = file("util-core"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
      name := "util-core"
    )

  lazy val utilConfig = Project(
    id = "util-config",
    base = file("util-config"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
      name := "util-config"
    ).dependsOn(utilCore  % "test->test;compile->compile")


  lazy val utilHttp = Project(
    id = "util-http",
    base = file("util-http"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
      name := "util-http"
    )

}

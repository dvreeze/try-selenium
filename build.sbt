
val scalaVer = "3.1.0"
val crossScalaVer = Seq(scalaVer)

ThisBuild / description  := "Trying out Selenium"
ThisBuild / organization := "eu.cdevreeze.tryselenium"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion       := scalaVer
ThisBuild / crossScalaVersions := crossScalaVer

ThisBuild / scalacOptions ++= Seq("-unchecked", "-indent", "-new-syntax", "-Xfatal-warnings")

ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

ThisBuild / pomExtra := pomData
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "4.1.1"

ThisBuild / libraryDependencies += "io.github.bonigarcia" % "webdrivermanager" % "5.0.3"

ThisBuild / libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test

lazy val root = project.in(file("."))
  .settings(
    name                 := "tryselenium",
    publish              := {},
    publishLocal         := {},
    publishArtifact      := false,
    Keys.`package`       := file(""))

lazy val pomData =
  <url>https://github.com/dvreeze/tryselenium</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
      <comments>Try-selenium is licensed under Apache License, Version 2.0</comments>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:dvreeze/tryselenium.git</connection>
    <url>https://github.com/dvreeze/tryselenium.git</url>
    <developerConnection>scm:git:git@github.com:dvreeze/tryselenium.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>dvreeze</id>
      <name>Chris de Vreeze</name>
      <email>chris.de.vreeze@caiway.net</email>
    </developer>
  </developers>


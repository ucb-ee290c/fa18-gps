name := "ee290c"

organization := "edu.berkeley.cs"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "rocket-dsptools" % "1.2-102318-SNAPSHOT"
)

scalaVersion := "2.12.6"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

scalacOptions ++= Seq(
  "-Xsource:2.11",
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-language:reflectiveCalls",
  "-Xcheckinit",
  "-Xlint:infer-any",
  "-Xlint:missing-interpolator",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-value-discard",
)

parallelExecution in Test := false

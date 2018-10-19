name := "ee290c"

organization := "edu.berkeley.cs"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "dsptools" % "1.1.2"
)

scalaVersion := "2.12.6"

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

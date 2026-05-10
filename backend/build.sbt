val scala3Version = "3.8.3"
val pekkoVersion = "1.6.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-poker",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    )
  )

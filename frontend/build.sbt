ThisBuild / scalaVersion := "3.3.3"

val circeVersion = "0.14.15"
val pekkoVersion = "1.6.0"
val pekkoHttpVersion = "1.3.0"

lazy val frontend = (project in file("."))
  .settings(
    name := "poker-frontend",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),
    Compile / mainClass := Some("poker.frontend.PokerApp"),
  )

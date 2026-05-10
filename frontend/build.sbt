ThisBuild / scalaVersion := "3.3.3"

lazy val frontend = (project in file("."))
  .settings(
    name := "poker-frontend",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",
    Compile / mainClass := Some("poker.frontend.PokerApp"),
  )


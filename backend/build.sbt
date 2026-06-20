val scala3Version = "3.8.3"
val pekkoVersion = "1.6.0"
val pekkoHttpVersion = "1.3.0"
val circeVersion   = "0.14.15"

Compile / scalacOptions ++= Seq(
  "-explain-cyclic",
  "-deprecation",
  "-feature",
  "-unchecked"
)

Compile / mainClass := Some("poker.Main")

assembly / mainClass := Some("poker.Main")
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-poker",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,
    
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
      
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-core" % pekkoHttpVersion,
      
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      
      "ch.qos.logback" % "logback-classic" % "1.5.6",
    )
  )

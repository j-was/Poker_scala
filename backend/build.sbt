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

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-poker",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    enablePlugins(JavaAppPackaging),

    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),

      // ── Pekko (actor system) ──────────────────────────────────────────────────
      libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed"         % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"              % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream-typed"        % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j"               % pekkoVersion,
    ),

    // ── Pekko HTTP (WebSocket server) ─────────────────────────────────────────
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-core" % pekkoHttpVersion,
    ),

    // ── JSON (circe) ──────────────────────────────────────────────────────────
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
    ),

    // ── Logging ───────────────────────────────────────────────────────────────
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
    ),
  )

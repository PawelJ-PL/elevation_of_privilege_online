val dependencies = {
  val plugins = Seq(
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)
  )

  val logger = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1",
    "org.logback-extensions" % "logback-ext-loggly" % "0.1.5",
    "org.codehaus.janino" % "janino" % "3.1.3"
  )

  val zio = Seq(
    "dev.zio" %% "zio" % "1.0.5",
    "dev.zio" %% "zio-streams" % "1.0.5",
    "dev.zio" %% "zio-interop-cats" % "2.3.1.0"
  ) ++ Seq(
    "dev.zio" %% "zio-config",
    "dev.zio" %% "zio-config-magnolia",
    "dev.zio" %% "zio-config-typesafe"
  ).map(_ % "1.0.2") ++ Seq(
    "dev.zio" %% "zio-logging",
    "dev.zio" %% "zio-logging-slf4j"
  ).map(_ % "0.5.8") ++ Seq(
    "io.github.gaelrenoux" %% "tranzactio"
  ).map(_ % "1.2.0") ++ Seq(
    "com.gh.dobrynya" %% "zio-jms" % "0.1"
  ) ++ Seq(
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt",
    "dev.zio" %% "zio-test-magnolia"
  ).map(_ % "1.0.5" % "test")

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % "0.13.0") ++ Seq(
    "io.circe" %% "circe-literal"
  ).map(_ % "0.13.0" % "test")

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-circe"
  ).map(_ % "0.21.20")

  val fuuid = Seq(
    "io.chrisdavenport" %% "fuuid",
    "io.chrisdavenport" %% "fuuid-circe",
    "io.chrisdavenport" %% "fuuid-http4s"
  ).map(_ % "0.5.0")

  val tsec = Seq(
    "io.github.jmcardon" %% "tsec-common",
    "io.github.jmcardon" %% "tsec-jwt-mac"
  ).map(_ % "0.2.1")

  val snakeyaml = Seq(
    "org.yaml" % "snakeyaml" % "1.28"
  )

  val database = Seq(
    "org.postgresql" % "postgresql" % "42.2.19",
    "org.liquibase" % "liquibase-core" % "4.3.1"
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-quill",
    "org.tpolecat" %% "doobie-postgres"
  ).map(_ % "0.12.1")

  val chimney = Seq(
    "io.scalaland" %% "chimney" % "0.6.1"
  )

  val enumeratum = Seq(
    "com.beachape" %% "enumeratum" % "1.6.1",
    "com.beachape" %% "enumeratum-circe" % "1.6.1",
    "com.beachape" %% "enumeratum-cats" % "1.6.1"
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % "2.5.3"
  )

  val artemis = Seq(
    "org.apache.activemq" % "artemis-jms-client" % "2.17.0"
  )

  val tests = Seq(
    "org.scalatest" %% "scalatest" % "3.2.6" % Test
  )

  libraryDependencies ++= plugins ++ logger ++ zio ++ circe ++ http4s ++ fuuid ++ tsec ++ snakeyaml ++ database ++ doobie ++ chimney ++ enumeratum ++ fs2 ++ artemis ++ tests
}

val compilerOptions = scalacOptions -= "-Xfatal-warnings"

val root = (project in file("."))
  .settings(
    name := "eop_online",
    scalaVersion := "2.13.5",
    compilerOptions,
    dependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers += Resolver.bintrayRepo("dobrynya", "maven"),
    useJGit
  )
  .enablePlugins(GitVersioning)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

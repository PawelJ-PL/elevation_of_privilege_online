val dependencies = {
  val plugins = Seq(
    compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )

  val logger = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
  )

  val zio = Seq(
    "dev.zio" %% "zio" % "1.0.1",
    "dev.zio" %% "zio-streams" % "1.0.1",
    "dev.zio" %% "zio-interop-cats" % "2.1.4.0+4-05a4920e-SNAPSHOT" //FIXME
  ) ++ Seq(
    "dev.zio" %% "zio-config",
    "dev.zio" %% "zio-config-magnolia",
    "dev.zio" %% "zio-config-typesafe"
  ).map(_ % "1.0.0-RC27") ++ Seq(
    "dev.zio" %% "zio-logging",
    "dev.zio" %% "zio-logging-slf4j"
  ).map(_ % "0.5.1") ++ Seq(
    "io.github.gaelrenoux" %% "tranzactio"
  ).map(_ % "1.0.0") ++ Seq(
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt",
    "dev.zio" %% "zio-test-magnolia"
  ).map(_ % "1.0.1" % "test")

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic"
  ).map(_ % "0.13.0") ++ Seq(
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-literal"
  ).map(_ % "0.13.0" % "test")

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-circe"
  ).map(_ % "0.21.7")

  val fuuid = Seq(
    "io.chrisdavenport" %% "fuuid",
    "io.chrisdavenport" %% "fuuid-circe",
    "io.chrisdavenport" %% "fuuid-http4s"
  ).map(_ % "0.4.0")

  val tsec = Seq(
    "io.github.jmcardon" %% "tsec-common",
    "io.github.jmcardon" %% "tsec-jwt-mac"
  ).map(_ % "0.2.1")

  val snakeyaml = Seq(
    "org.yaml" % "snakeyaml" % "1.27"
  )

  val database = Seq(
    "org.postgresql" % "postgresql" % "42.2.16",
    "org.liquibase" % "liquibase-core" % "4.0.0"
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-quill",
    "org.tpolecat" %% "doobie-postgres"
  ).map(_ % "0.9.2")

  val chimney = Seq(
    "io.scalaland" %% "chimney" % "0.5.3"
  )

  val enumeratum = Seq(
    "com.beachape" %% "enumeratum" % "1.6.1",
    "com.beachape" %% "enumeratum-circe" % "1.6.1"
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % "2.4.4"
  )

  libraryDependencies ++= plugins ++ logger ++ zio ++ circe ++ http4s ++ fuuid ++ tsec ++ snakeyaml ++ database ++ doobie ++ chimney ++ enumeratum ++ fs2
}

val compilerOptions = scalacOptions -= "-Xfatal-warnings"

val root = (project in file("."))
  .settings(
    name := "eop_online",
    version := "0.1", //TODO: replace with git version
    scalaVersion := "2.13.3",
    compilerOptions,
    dependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers += Resolver.mavenLocal //FIXME
  )

resourceGenerators in Compile += Def.task {
  val distDir = baseDirectory.value / "frontend" / "eop_online" / "build"
  val resourceDir = resourceManaged.value / "main" / "static"

  for {
    (from, to) <- distDir ** "*" pair Path.rebase(distDir, resourceDir)
  } yield {
    Sync.copy(from, to)
    to
  }
}.taskValue

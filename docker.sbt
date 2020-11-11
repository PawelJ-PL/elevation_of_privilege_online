packageName in Docker := "paweljpl/eop-online"
maintainer in Docker := "Pawel <inne.poczta@gmail.com>"

dockerBaseImage := "openjdk:11-jre-slim"
dockerExposedPorts ++= Seq(8181, 8181)

dockerUpdateLatest := true

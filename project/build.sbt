val circeVersion = "0.11.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

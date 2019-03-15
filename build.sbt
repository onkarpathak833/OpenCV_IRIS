name := "IRIS"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.3.0",
  "org.apache.spark" %% "spark-sql" % "2.3.0",
  "com.typesafe" % "config" % "1.3.2",
  "org.openpnp" % "opencv" % "3.2.0-1",
  "org.apache.spark" %% "spark-mllib" % "2.3.0"
)

resolvers += Resolver.mavenLocal
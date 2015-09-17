import _root_.sbt.Keys._
import _root_.sbt.{TestFrameworks, Tests}

name := "akka-mini-p2p-scala"

version := "1.0"

scalaVersion := "2.11.6"

val akkaVersion = "2.3.11"

javaOptions in run ++= Seq(
    "-Djava.library.path=./sigar",
    "-Xms128m", "-Xmx1024m")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "junit" % "junit" % "4.12" % "test",
    "com.novocode" % "junit-interface" % "0.11" % "test",
    "com.typesafe.akka" % "akka-slf4j_2.11" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "org.fusesource" % "sigar" % "1.6.4"
)



fork in run := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
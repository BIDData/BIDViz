name := "BIDMach_Viz"

version := "1.1.1"
organization := "edu.berkeley.bid"
scalaVersion := "2.11.7"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  "../../BIDMach-ui.jar"
}

resolvers ++= Seq(
  "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
  "Scala Mirror" at "https://oss.sonatype.org/content/repositories/releases/"
//  "BIDMat" at "https://dl.bintray.com/biddata/BIDData/"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
// javaHome := Some(file("/usr/lib/jvm/java-7-oracle"))

// scalacOptions ++= Seq("-deprecation","-target:jvm-1.7")

initialCommands := scala.io.Source.fromFile("lib/bidmach_init.scala").getLines.mkString("\n")
javaOptions += "-Xmx20g"

// https://mvnrepository.com/artifact/org.scala-lang/scala-compiler
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.8"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor_2.11

val AkkaVersion = "2.4.9"
val akkaHttpJson = "com.typesafe.akka" %% "akka-http-spray-json-experimental" % AkkaVersion
val akkaActor = "com.typesafe.akka" %% "akka-actor" % AkkaVersion
val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % AkkaVersion
libraryDependencies += akkaHttpJson
libraryDependencies += akkaActor
libraryDependencies += akkaStream
libraryDependencies += akkaHttp
// https://mvnrepository.com/artifact/com.typesafe.play/play_2.11
libraryDependencies += "com.typesafe.play" % "play_2.11" % "2.4.8"
libraryDependencies += "com.typesafe.play" % "play-server_2.11" % "2.4.8"
libraryDependencies += "com.typesafe.play" % "play-netty-server_2.11" % "2.4.8"
libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.8"
// libraryDependencies += "BIDMat" % "BIDMat" % "1.1.1"
// libraryDependencies += "BIDMach" % "BIDMach" % "1.1.1"
libraryDependencies += "com.googlecode.scalascriptengine" % "scalascriptengine" % "1.3.6-2.10.3"


unmanagedResourceDirectories in Compile += baseDirectory.value / "lib"
includeFilter in (Compile, unmanagedResourceDirectories):= ".dll,.so"


// to be able to get realtime compilation working in sbt
// see
// https://github.com/kostaskougios/scalascriptengine/issues/13
val sbtcp = taskKey[Unit]("sbt-classpath")
sbtcp := {
  val files: Seq[File] = (fullClasspath in Compile).value.files
  val sbtClasspath : String = files.map(_.getAbsolutePath).mkString(":")
 // val sbtClasspath : String = files.map(x => x.getAbsolutePath).mkString(":")
  println("Set SBT classpath to 'sbt-classpath' environment variable" + sbtClasspath)
  System.setProperty("c", sbtClasspath)
}

// compile  <<= (compile in Compile).dependsOn(sbtcp)
run <<= (run in Runtime).dependsOn(sbtcp)

//fork := true

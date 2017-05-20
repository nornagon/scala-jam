import Dependencies._
enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.2",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    resolvers += Resolver.mavenLocal,
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.processing" % "core" % "3.2.3",
    libraryDependencies += "org.lwjgl" % "lwjgl-opengl" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl-opengl" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.lwjgl" % "lwjgl-glfw" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl-glfw" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.lwjgl" % "lwjgl" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "skia" % "20170511-53d6729-1.3.3-SNAPSHOT",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "skia" % "20170511-53d6729-1.3.3-SNAPSHOT" classifier "macosx-x86_64" classifier "linux-x86_64",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "liquidfun" % "20150401-0708ce1-1.3.3-SNAPSHOT",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "liquidfun" % "20150401-0708ce1-1.3.3-SNAPSHOT" classifier "macosx-x86_64" classifier "linux-x86_64",
    libraryDependencies ++= Seq(
      "net.nornagon" %% "scanvas-core" % "0.1.0-SNAPSHOT",
      "net.nornagon" %% "scanvas-gpu" % "0.1.0-SNAPSHOT"
    ),
    javaOptions in run := Seq("-XstartOnFirstThread"),  // required by glfw
    fork in run := true
)

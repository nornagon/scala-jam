enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.2",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Scala Jam",
    resolvers += Resolver.mavenLocal,
    libraryDependencies += "org.lwjgl" % "lwjgl-opengl" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl-opengl" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.lwjgl" % "lwjgl-glfw" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl-glfw" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.lwjgl" % "lwjgl" % "3.1.1",
    libraryDependencies += "org.lwjgl" % "lwjgl" % "3.1.1" classifier "natives-macos",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "skia" % "20170511-53d6729-1.3.3-SNAPSHOT",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "skia" % "20170511-53d6729-1.3.3-SNAPSHOT" classifier "linux-x86_64",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "liquidfun" % "20150401-0708ce1-1.3.3-SNAPSHOT",
    libraryDependencies += "org.bytedeco.javacpp-presets" % "liquidfun" % "20150401-0708ce1-1.3.3-SNAPSHOT" classifier "linux-x86_64",
    libraryDependencies ++= Seq(
      "net.nornagon" %% "scanvas-core" % "0.1.0-SNAPSHOT",
      "net.nornagon" %% "scanvas-gpu" % "0.1.0-SNAPSHOT",
      "net.nornagon" %% "kit" % "0.1.0-SNAPSHOT"
    ),
    fork in run := true
)

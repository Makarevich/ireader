scalaVersion := "2.10.0"

conflictManager := ConflictManager.strict

libraryDependencies ++= Seq(
    "com.google.apis" % "google-api-services-drive" % "v2-rev145-1.19.0",
    "com.google.http-client" % "google-http-client-jackson" % "1.19.0",
    "net.liftweb" % "lift-json_2.10" % "2.6-M3",
    "org.fusesource.scalate" % "scalate-page_2.10" % "1.6.1",
    "org.slf4j" % "slf4j-jdk14" % "1.6.1",
    "javax.servlet" % "javax.servlet-api" % "3.1.0",
    // "org.webjars" % "jquery" % "2.1.1",
    "org.webjars" % "foundation" % "5.4.5")

seq(coffeeSettings:_*)

tomcat()

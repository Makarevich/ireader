organization := "slick"

name := "ireader"

version := "0.1.0"

scalaVersion := "2.11.2"

jetty()

libraryDependencies ++= Seq(
    "com.google.apis" % "google-api-services-drive" % "v2-rev145-1.19.0",
    "com.google.http-client" % "google-http-client-jackson" % "1.19.0",
    "net.debasishg" %% "redisclient" % "2.13",
    // "org.webjars" % "jquery" % "2.1.1",
    "org.webjars" % "foundation" % "5.4.5",
    "org.webjars" % "foundation-icon-fonts" % "d596a3cfb3",
    "org.webjars" % "angularjs" % "1.3.2",
    "org.scalatra" %% "scalatra" % "2.3.0",
    "org.scalatra" %% "scalatra-scalate" % "2.3.0",
    "org.scalatra" %% "scalatra-json" % "2.3.0",
    "org.scalatra" %% "scalatra-specs2" % "2.3.0" % "test",
    "org.json4s"   %% "json4s-jackson" % "3.2.9",
    "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
    "org.eclipse.jetty" % "jetty-webapp" % "9.2.1.v20140609" % "container",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"
)

seq(coffeeSettings: _*)

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (target in Compile)(_ / "webapp" / "js")

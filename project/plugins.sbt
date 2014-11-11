addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "1.0.0-M6")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")

resolvers += Resolver.url("sbt-plugin-snapshots",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("com.bowlingx" %% "xsbt-wro4j-plugin" % "0.3.5")

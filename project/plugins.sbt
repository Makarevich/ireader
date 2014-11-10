addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "1.0.0")

addSbtPlugin("me.lessis" % "coffeescripted-sbt" % "0.2.3")

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
      Resolver.ivyStylePatterns)

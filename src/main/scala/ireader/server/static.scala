package ireader.server

import org.scalatra._
import scalate.ScalateSupport

class StaticSvlt extends ScalatraServlet with ScalateSupport {
  notFound {
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}

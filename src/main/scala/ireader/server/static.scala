package ireader.server

import org.scalatra._
import scalate.ScalateSupport

class StaticSvlt extends ScalatraServlet with ScalateSupport {
    get("/") {
        contentType = "text/html"
        jade("folder")
    }

    get("/doc") {
        contentType = "text/html"
        jade("doc")
    }

    get("/queue") {
        contentType = "text/html"
        jade("queue")
    }

    notFound {
        serveStaticResource() getOrElse resourceNotFound()
    }
}

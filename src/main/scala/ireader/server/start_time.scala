package ireader.server

import javax.servlet.http._
import org.scalatra.ScalatraServlet

class StartTimeSvlt extends ScalatraServlet {
    post("/") {
        info("Started on")
        started_on
    }
}

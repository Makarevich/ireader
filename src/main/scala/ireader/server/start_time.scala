package ireader.server

import org.scalatra.ScalatraServlet

class StartTimeSvlt extends ScalatraServlet {
    post("/") {
        info("Started on")
        started_on
    }
}

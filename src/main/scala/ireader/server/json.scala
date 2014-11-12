package ireader.server

import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

import org.json4s.DefaultFormats

trait JsonSvlt extends ScalatraServlet with JacksonJsonSupport {
    protected implicit val jsonFormats = DefaultFormats

    def sess = new Session
}

package ireader.server

import javax.servlet.http._
import net.liftweb.json._

abstract class JsonServlet extends HttpServlet {
    protected var req: HttpServletRequest = null;
    protected var resp: HttpServletResponse = null;
    protected def session = this.req.getSession

    protected def getReqParam(name: String): Option[String] = {
        val v = this.req.getParameter(name)
        if(v == null) None else Some(v)
    }

    protected def doGet: JValue = ???
    protected def doPost(data: JValue): JValue = ???

    override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        this.req = req
        this.resp = resp

        val json = doGet

        resp.setContentType("application/json")
        resp.getOutputStream.println(compact(render(json)))
    }

    override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        this.req = req
        this.resp = resp

        val data: String = {
            val reader = req.getReader
            Stream.continually(reader.readLine).takeWhile(_ != null).mkString
        }

        val json = doPost(parse(data))

        resp.setContentType("application/json")
        resp.getOutputStream.println(compact(render(json)))
    }

}


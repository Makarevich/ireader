package ireader.server

import javax.servlet.http._
import net.liftweb.json._

abstract class JsonServlet extends HttpServlet {
    protected var req: HttpServletRequest = null;
    protected var resp: HttpServletResponse = null;
    protected var session: Session = null

    protected def getReqParam(name: String): Option[String] =
        Option(this.req.getParameter(name))

    protected def doGet: JValue = ???
    protected def doPost(data: JValue): JValue = ???

    protected def initRequest(req: HttpServletRequest, resp: HttpServletResponse) {
        this.req = req
        this.resp = resp
        this.session = new Session(this.req.getSession)
    }

    override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        initRequest(req, resp)

        val json = doGet

        resp.setContentType("application/json")
        resp.setCharacterEncoding("UTF-8")
        resp.getWriter.print(compact(render(json)))
    }

    override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        initRequest(req, resp)

        val data: String = {
            val reader = req.getReader
            Stream.continually(reader.readLine).takeWhile(_ != null).mkString
        }

        val json = doPost(parse(data))

        resp.setContentType("application/json")
        resp.getOutputStream.println(compact(render(json)))
    }

}


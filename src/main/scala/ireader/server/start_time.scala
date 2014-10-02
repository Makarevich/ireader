package ireader.server

import javax.servlet.http._

class StartTimeSvlt extends HttpServlet {
    override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.setContentType("text/plain")
        resp.getOutputStream.println(started_on)
    }
}

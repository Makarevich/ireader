package ireader.server

import javax.servlet.http._

class InitServlet extends HttpServlet {
    override def init {
        info("Initializing ireader...")
    }
}


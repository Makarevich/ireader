package ireader.server

import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

import org.json4s.DefaultFormats

trait JsonSvlt extends ScalatraServlet with JacksonJsonSupport {
    protected implicit val jsonFormats = DefaultFormats

    private def session_data = new SessionData
    private def sess_state_mgr = new SessionStateManager(
        linker.TokenContainerFactory,
        linker.GoogleDriveFactory,
        session_data)
    protected def sess = sess_state_mgr.get
    protected def update_session(token: String) = sess_state_mgr.set(token)


    before() {
        contentType = formats("json")
    }
}

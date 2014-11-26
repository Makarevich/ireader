package ireader.server

import javax.servlet.http.HttpSession

import ireader.drive._
import ireader.drive.web._

class SessionStateManager(token_factory: ITokenContainerFactory,
                          session_state_factory: ISessionStateFactory,
                          session_data: SessionData)
{
    def get: ISessionState = {
        session_data.drive.getOption.getOrElse {
            update_session(token_factory.make)
        }
    }

    def set(new_token: String): ISessionState = {
        update_session(token_factory.make(new_token))
    }

    private def update_session(token_box: ITokenContainer): ISessionState = {
        val new_state = session_state_factory.build(token_box)
        session_data.drive.set(new_state)
        new_state
    }
}


class SessionData (implicit servlet_session: HttpSession) {
    val drive = new SessionData.Item[ISessionState]("drive")
}

object SessionData {
    class Item[T](key: String)(implicit servlet_session: HttpSession) {
        def getOption: Option[T] = Option {
            servlet_session.getAttribute(key).asInstanceOf[T]
        }
        def get: T = getOption.get
        def set(value: T) {
            servlet_session.setAttribute(key, value)
        }
        def remove: Unit = servlet_session.removeAttribute(key)
    }

}

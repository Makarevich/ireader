package ireader.server

import javax.servlet.http.HttpSession

import ireader.drive._

class SessionStateManager(token_factory: ITokenContainerFactory,
                          drive_factory: IGoogleDriveFactory,
                          session_data: SessionData)
{
    def get: SessionState = {
        session_data.drive.getOption.getOrElse {
            update_session(token_factory.make)
        }
    }

    def set(new_token: String): SessionState = {
        update_session(token_factory.make(new_token))
    }

    private def update_session(token_box: ITokenContainer): SessionState = {
        val new_state = new SessionState(token_box, drive_factory)
        session_data.drive.set(new_state)
        new_state
    }
}


class SessionData (implicit servlet_session: HttpSession) { sess =>
    val drive = new SessionData.Item[SessionState]("drive")
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

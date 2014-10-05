package ireader.server

import javax.servlet.http.HttpSession

import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.drive.Drive

class Session(servlet_session: HttpSession) {
    class Item[T](key: String) {
        def getOption: Option[T] = Option {
            servlet_session.getAttribute(key).asInstanceOf[T]
        }
        def get: T = getOption.get
        def set(value: T): Unit = servlet_session.setAttribute(key, value)
        def remove: Unit = servlet_session.removeAttribute(key)
    }

    val redirect_to = new Item[String]("redirect_to")
    val access_token = new Item[String]("access_token")
    val google_creds = new Item[Credential]("google_credential")
    val drive = new Item[Drive]("drive")
}

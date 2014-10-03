package ireader.server

import javax.servlet.http._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory

import com.google.api.services.drive.DriveScopes


object AuthSvlt {
    private val CLIENT_ID = "1033390415538-tfko6f392unt8drju50i763vfc5sr6v5.apps.googleusercontent.com"
    private val CLIENT_SECRET = "VuGj-ju_qaYYQECyqgvaBBXj"
}

class AuthSvlt extends JsonServlet {
    import collection.JavaConversions._

    private lazy val auth_flow =
        new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport,
            new JacksonFactory,
            AuthSvlt.CLIENT_ID,
            AuthSvlt.CLIENT_SECRET,
            List(DriveScopes.DRIVE_READONLY)).build

    private def augmentRediectTo(base_url: String): String = base_url + "auth"

    override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val auth_code = req.getParameter("code")
        assert(auth_code != null)

        val redirect_to: String = req.getSession.getAttribute(
            Session.SESSION_REDIRECT_TO).toString

        val token_response = auth_flow.newTokenRequest(auth_code)
                                      .setRedirectUri(augmentRediectTo(redirect_to))
                                      .execute
        val access_token: String = token_response.getAccessToken
        info(s"Access token: ${access_token}")
        req.getSession.setAttribute(Session.SESSION_ACCESS_TOKEN, access_token)

        val creds = auth_flow.createAndStoreCredential(token_response, null)
        req.getSession.setAttribute(Session.SESSION_GOOGLE_CREDS, creds)

        resp.sendRedirect(resp.encodeRedirectURL(redirect_to))
    }

    override def doPost(data: JValue): JValue = {
        val access_token = session.getAttribute(Session.SESSION_ACCESS_TOKEN)
        if (access_token != null) {
            return ("access_token" -> access_token.toString)
        }

        val JString(redirect_to: String) = data \ "redirect_to"
        session.setAttribute(Session.SESSION_REDIRECT_TO, redirect_to)
        val auth_url = auth_flow.newAuthorizationUrl
                                .setRedirectUri(augmentRediectTo(redirect_to))
                                .build
        ("redirect_to" -> auth_url)
    }
}

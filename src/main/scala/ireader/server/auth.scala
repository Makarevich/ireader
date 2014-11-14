package ireader.server

import org.json4s._
import org.json4s.JsonDSL._

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow

import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes


class AuthSvlt extends JsonSvlt {
    private def augmentRediectTo(s: String) = s + "auth"

    private def redirect_url = fullUrl("/")

    get("/") {
        params.get("code") match {
        case Some(auth_code) =>
            val token_response = AuthSvlt.auth_flow.newTokenRequest(auth_code)
                                                   .setRedirectUri(redirect_url)
                                                   .execute
            sess.access_token.set(token_response.getAccessToken)
            redirect(url(""))
        case None =>
            val is_ok = {
                val is_force = params.get("force") match {
                    case Some("true") => true
                    case _ => false
                }
                val drive_opt = sess.drive.getOption
                is_force == false && !drive_opt.isEmpty
            }

            if(is_ok) redirect(url("")) else {
                val auth_url = AuthSvlt.auth_flow.newAuthorizationUrl
                                                 .setRedirectUri(redirect_url)
                                                 .build
                redirect(auth_url)
            }
        }
    }
}

object AuthSvlt {
    import collection.JavaConversions._

    private val CLIENT_ID = "1033390415538-tfko6f392unt8drju50i763vfc5sr6v5.apps.googleusercontent.com"
    private val CLIENT_SECRET = "VuGj-ju_qaYYQECyqgvaBBXj"

    lazy val auth_flow =
        new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport,
            new JacksonFactory,
            AuthSvlt.CLIENT_ID,
            AuthSvlt.CLIENT_SECRET,
            List(DriveScopes.DRIVE_READONLY)).build

}

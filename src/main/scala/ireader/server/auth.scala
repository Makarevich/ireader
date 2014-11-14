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

    before() {
        contentType = formats("json")
    }

    val auth_route = get("/") {
        val auth_code = params.get("code")
        assert(!auth_code.isEmpty)

        val redirect_to: String = sess.redirect_to.get

        val token_response = AuthSvlt.auth_flow.newTokenRequest(auth_code.get)
                                               .setRedirectUri(augmentRediectTo(redirect_to))
                                               .execute

        sess.access_token.set(token_response.getAccessToken)

        // {
        //     val access_token: String = token_response.getAccessToken
        //     info(s"Token response: ${token_response.toPrettyString}")
        //     info(s"Access token: ${access_token}")
        // }

        // val creds = auth_flow.createAndStoreCredential(token_response, null)
        // sess.drive.set(new Drive.Builder(new NetHttpTransport,
        //                                  new JacksonFactory,
        //                                  creds).build)

        info(s"Redirecting to ${redirect_to}")
        redirect(redirect_to)
    }

    post("/") {
        val is_ok = {

            val is_force: Boolean =  params.get("force") match {
                case Some("true") => true
                case _ => false
            }
            info("Checking drive")
            val drive_opt = sess.drive.getOption
            is_force == false && !drive_opt.isEmpty
        }
        if(is_ok) ("result" -> "OK") else {
            val redirect_to = params("redirect_to")
            sess.redirect_to.set(redirect_to)
            val augmented_redirect = augmentRediectTo(redirect_to)
            val auth_url = AuthSvlt.auth_flow.newAuthorizationUrl
                                             .setRedirectUri(augmented_redirect)
                                             .build
            ("redirect_to" -> auth_url)
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

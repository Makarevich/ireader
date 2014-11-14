package ireader.server

import javax.servlet.http.HttpSession
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse

// import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.drive.Drive

class Item[T](key: String)(implicit servlet_session: HttpSession) {
    def getOption: Option[T] = Option {
        servlet_session.getAttribute(key).asInstanceOf[T]
    } orElse {
        val res = try_build
        res.foreach(x => store_in_session(x))
        res
    }
    def get: T = getOption.get
    def set(value: T) {
        store_in_session(value)
        store_value(value)
    }
    def remove: Unit = servlet_session.removeAttribute(key)

    private def store_in_session(value: T) {
        servlet_session.setAttribute(key, value)
    }

    protected def try_build: Option[T] = None
    protected def store_value(value: T) { }
}

class Session(implicit servlet_session: HttpSession) { sess =>
    // val redirect_to = new Item[String]("redirect_to")
    // val google_creds = new Item[Credential]("google_credential")
    // val token_response = new Item[GoogleTokenResponse]("token_response") { }
    val access_token = new Item[String]("access_token") {
        import com.redis.RedisClient

        private def client = new RedisClient

        override def try_build: Option[String] = {
            val result = client.get(Session.REDIS_KEY)
            result match {
                case Some(_) => info("Fetched access token")
                case None => info("Fetched NO token")
            }
            result
        }
        override def store_value(value: String) {
            info("Setting to redis")
            client.set(Session.REDIS_KEY, value)
        }
    }

    val drive = new Item[Drive]("drive") {
        override def try_build: Option[Drive] = {
            sess.access_token.getOption.map { access_token =>
                val token_response = new TokenResponse
                token_response.setAccessToken(access_token)
                val creds = AuthSvlt.auth_flow.createAndStoreCredential(
                                token_response, null)
                new Drive.Builder(new NetHttpTransport,
                                  new JacksonFactory,
                                  creds).build
            }
        }
    }
}

object Session {
    private val REDIS_KEY = "ireader_access_token"
}

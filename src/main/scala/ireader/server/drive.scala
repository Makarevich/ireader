package ireader.server

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.About



class DriveSvlt extends JsonServlet {
    import collection.JavaConversions._

    private lazy val drive: Drive = {
        val creds = session.getAttribute(Session.SESSION_GOOGLE_CREDS)
        assert(creds != null)

        new Drive.Builder(
            new NetHttpTransport,
            new JacksonFactory,
            creds.asInstanceOf[Credential]).build
    }

    override def doGet: JValue = {
        val children = drive.children.list("root").execute

        val result = children.getItems.map { ch => drive.files.get(ch.getId).execute }
              .map{ f => ("title" -> f.getTitle) ~ ("link" -> f.getAlternateLink) }

        ( "children" -> result )
    }
}

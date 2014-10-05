package ireader.server

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.drive.model.About



class DriveSvlt extends JsonServlet {
    import collection.JavaConversions._

    override def doGet: JValue = {
        val drive = session.drive.get
        val children = drive.children.list("root").setQ("trashed = false").execute
        val result = children.getItems.map { ch => drive.files.get(ch.getId).execute }
            .map{ f =>
                ("title" -> f.getTitle) ~
                ("link" -> f.getAlternateLink)
                /* ("parents" -> f.getParents.map(_.getId)) */
            }

        ( "children" -> result )
    }
}

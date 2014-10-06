package ireader.server

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.drive.model.About



class DriveSvlt extends JsonServlet {
    import collection.JavaConversions._

    override def doPost(req: JValue): JValue = {
        val folder_id: String = req \ "folder_id" match {
            case JString(s) => s
            case JNothing => "root"
            case _ => ???
        }

        val drive = session.drive.get
        val children = drive.children.list(folder_id).setQ("trashed = false").execute
        val resolved = children.getItems.par.map { ch => drive.files.get(ch.getId).execute }

        val (folders, files) = resolved.seq.sortBy(_.getTitle).par.partition(_.getMimeType == DriveSvlt.FOLDER_MIME)

        ("folders" -> folders.map { f=>
            ("title" -> f.getTitle) ~
            ("id" -> f.getId)
        }.seq) ~
        ("files" -> files.map { f =>
            ("title" -> f.getTitle) ~
            ("link" -> f.getAlternateLink)
        }.seq)
    }
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
}

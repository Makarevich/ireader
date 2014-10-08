package ireader.server

import concurrent._
import concurrent.duration._

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.services.drive.model.{About, File}

import ireader.utils.DriveBatcher



class DriveSvlt extends JsonServlet {
    import collection.JavaConversions._
    import ExecutionContext.Implicits.global

    override def doPost(req: JValue): JValue = {
        val folder_id: String = req \ "folder_id" match {
            case JString(s) => s
            case JNothing => "root"
            case _ => ???
        }

        val (folders_par, files_par) = {
            val batcher = DriveBatcher(session.drive.get)
            val children = batcher.single { drive =>
                drive.children.list(folder_id)
                              .setQ("trashed = false")
            }
            val resolved = batcher.multiple { drive =>
                children.getItems.map { ch => drive.files.get(ch.getId) }
            }
            resolved.sortBy(_.getTitle)
                    .par.partition(_.getMimeType == DriveSvlt.FOLDER_MIME)
        }

        val folders_json_future = future { folders_par.map { f=>
            ("title" -> f.getTitle) ~
            ("id" -> f.getId)
        }.seq }
        val files_json_future = future { files_par.map { f =>
            ("title" -> f.getTitle) ~
            ("link" -> f.getAlternateLink)
        }.seq }

        ("folders" -> Await.result(folders_json_future, 30.seconds)) ~
        ("files" -> Await.result(files_json_future, 30.seconds))
    }
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
}

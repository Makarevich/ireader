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
            val drive = session.drive.get
            val children = drive.children.list(folder_id)
                                         .setQ("trashed = false")
                                         .execute

            val resolved_par = {
                val batch = drive.batch
                val file_builder = List.newBuilder[File]
                val batch_callback = new JsonBatchCallback[File] {
                    def onSuccess(f: File, headers: HttpHeaders) {
                        file_builder += f
                    }
                    def onFailure(err: GoogleJsonError, headers: HttpHeaders): Unit = ???
                }

                children.getItems.foreach { ch =>
                    drive.files.get(ch.getId).queue(batch, batch_callback)
                }
                batch.execute
                file_builder.result
            }

            resolved_par.seq.sortBy(_.getTitle)
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

package ireader.server

import concurrent._
import concurrent.duration._

import org.json4s._
import org.json4s.JsonDSL._

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.services.drive.model.{About, File, Property}

import ireader.utils.DriveBatcher



class DriveSvlt extends JsonSvlt {
    import collection.JavaConversions._
    import ExecutionContext.Implicits.global

    post("/doc") {
        val JString(id) = parsedBody \ "id"

        val drive = sess.drive.get
        val batcher = DriveBatcher(drive)


        val f_prop_opt: Future[Option[Property]] = parsedBody \ "action" match {
        case JString("update") =>
            val JInt(base) = parsedBody \ "base"
            val JInt(halflife) = parsedBody \ "halflife"

            val prop = new Property
            prop.setKey(DriveSvlt.IMPORTANCE_PROP)
            prop.setValue(Seq(base, halflife).mkString(" "))

            batcher {
                drive.properties.update(id, prop.getKey, prop)
            }.future.map(x => Some(x))
        case JString("untrack") =>
            batcher {
                drive.properties.delete(id, DriveSvlt.IMPORTANCE_PROP)
            }
            Future.successful(None)
        case JNothing =>
            batcher {
                drive.properties.list(id)
            } map { props =>
                props.getItems.find(_.getKey == DriveSvlt.IMPORTANCE_PROP)
            }
        case _ =>
            throw new java.lang.RuntimeException("Unknown action value")
        }

        val f_base_json = batcher {
            drive.files.get(id)
        } map { file =>
            ("title" -> file.getTitle) ~
            ("parent" -> file.getParents.head.getId) ~
            ("view_link" -> file.getAlternateLink):JValue
        }

        val f_importance_json = f_prop_opt.map { prop_opt =>
            val json_opt = prop_opt.map { prop =>
                info("Extracting props")
                val Array(base, hl) = prop.getValue.split(" ").map(_.toFloat)
                ("base" -> base) ~
                ("halflife" -> hl):JValue
            }
            json_opt.getOrElse(JNothing)
        }

        batcher.execute

        val Seq(base_json, importance_json) = Await.result(Future.sequence{
            Seq(f_base_json, f_importance_json)
        }, 10.seconds)

        base_json merge importance_json
    }

    post("/folder") {
        val JString(folder_id: String) = parsedBody \ "folder_id"

        val drive = sess.drive.get
        val batcher = DriveBatcher(drive)

        val f_folders = batcher {
            drive.files.get(folder_id)
        } flatMap { folder =>
            info(s"Parent count1 ${folder.getParents.size}")
            Future.sequence {
                info(s"Parent count2 ${folder.getParents.size}")
                folder.getParents.map {
                    p => batcher(drive.files.get(p.getId)).future
                }
            } map { parent_list =>
                //info(s"Generating parents")
                ("folder_title" -> folder.getTitle) ~
                ("parents" -> parent_list.map { f =>
                    ("title" -> f.getTitle) ~
                    ("id" -> f.getId)
                }):JValue
            }
        }

        val f_children = batcher {
            drive.children.list(folder_id).setQ("trashed = false")
        } flatMap { children_list =>
            Future.sequence {
                children_list.getItems.map {
                    ch => batcher(drive.files.get(ch.getId)).future
                }
            }
        } map { children =>
            val (folders, files) =
                children.sortBy(_.getTitle)
                        .partition(_.getMimeType == DriveSvlt.FOLDER_MIME)
            //info(s"Generating children")
            ("files" -> files.map { f =>
                ("title" -> f.getTitle) ~
                ("id" -> f.getId)
            }) ~
            ("folders" -> folders.map { p =>
                ("title" -> p.getTitle) ~
                ("id" -> p.getId)
            }):JValue
        }

        batcher.execute

        val Seq(folders, children) = Await.result(Future.sequence {
            Seq(f_folders, f_children)
        }, 20.seconds)

        folders merge children
    }
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
    private val IMPORTANCE_PROP = "ireader_importance"
}

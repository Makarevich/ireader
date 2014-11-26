package ireader.server

import concurrent.Future

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.services.drive.model.{File, Property}

import ireader.drive.BaseDocRecord
import ireader.utils.getCurrentTime

class DriveSvlt extends JsonSvlt {
    import collection.JavaConversions._

    post("/folder") {
        val JString(folder_id: String) = parsedBody \ "folder_id"

        val drive = sess.drive

        val f_folders: Future[JValue] = drive.getFile(folder_id).flatMap {
        folder =>
            info(s"Parent count1 ${folder.getParents.size}")
            Future.sequence {
                info(s"Parent count2 ${folder.getParents.size}")
                folder.getParents.map {
                    p => drive.getFile(p.getId).future
                }
            } map { plist =>
                (folder, plist)
            }
        }.future.map { case (folder, parent_list) =>
            info(s"Generating parents")
            ("folder_title" -> folder.getTitle) ~
            ("parents" -> parent_list.map { f =>
                ("title" -> f.getTitle) ~
                ("id" -> f.getId)
            }):JValue
        }

        val f_children = drive.listFolderChildren(folder_id).flatMap { ids =>
            Future.sequence {
                ids.map {
                    id => drive.getFile(id).future
                }
            }
        }.future.map { children =>
            val (folders, files) =
                children.sortBy(_.getTitle)
                        .partition(_.getMimeType == DriveSvlt.FOLDER_MIME)
            info(s"Generating children")
            ("files" -> files.map { f =>
                ("title" -> f.getTitle) ~
                ("id" -> f.getId)
            }) ~
            ("folders" -> folders.map { p =>
                ("title" -> p.getTitle) ~
                ("id" -> p.getId)
            }):JValue
        }

        drive.execute

        f_folders zip f_children map { case (folders, children) =>
            folders merge children
        }
    }

    post("/doc") {
        val parsed = parsedBody
        val JString(id) = parsed \ "id"

        val f_base_json = sess.drive.getFile(id).future.map { file =>
            ("title" -> file.getTitle) ~
            ("parent" -> file.getParents.head.getId) ~
            ("view_link" -> file.getAlternateLink):JValue
        }

        sess.drive.execute

        val f_doc_record: Future[Option[BaseDocRecord]] = {
            lazy val stored_doc = sess.props.get(id)
            lazy val parsed_doc = {
                val JInt(base) = parsed \ "base"
                val JInt(half) = parsed \ "half"
                BaseDocRecord(base.toInt, half.toInt, getCurrentTime)
            }

            def set_and_to_opt(id: String, rec: BaseDocRecord) = {
                sess.props.set(id, rec).map(x => Some(x))
            }

            parsedBody \ "action" match {
            case JString("init") =>
                set_and_to_opt(id, parsed_doc)
            case JString("update") =>
                stored_doc.flatMap { opt =>
                    val old = opt.get
                    set_and_to_opt(id, parsed_doc.copy(ts=old.ts))
                }
            case JString("read") =>
                stored_doc.flatMap { opt =>
                    val old = opt.get
                    set_and_to_opt(id, old.copy(ts=getCurrentTime))
                }
            case JString("untrack") => ???
            case _ =>
                stored_doc
            }
        }

        val f_props_json = f_doc_record.map { record_opt =>
            val json_opt = record_opt.map { record =>
                val doc = record.inflate(getCurrentTime)
                info(s"Inflated: ${doc}")
                Extraction.decompose(doc)
            }
            json_opt.getOrElse(JNothing)
        }

        f_base_json zip f_props_json map {
        case (base, props) =>
            base merge props
        }
    }

    /*
    get("/queue") {
        val drive = sess.drive.get
        val batcher = DriveBatcher(drive)

        val f_file_jsons = batcher {
            val req = drive.files.list
            .setQ("trashed = false and " +
                  s"properties has { key='${DriveSvlt.IMPORTANCE_PROP}' } " +
                  s"and properties has { key='${DriveSvlt.TIMESTAMP_PROP}' }")
            info(s"Q = ${req.getQ}")
            req
        }.flatMap { file_list =>
            Future.sequence {
                file_list.getItems.map { file =>
                    val base =
                    ("title" -> file.getTitle) ~
                    ("parent" -> file.getParents.head.getId) ~
                    ("view_link" -> file.getAlternateLink):JValue

                    val f_imp = batcher {
                        drive.properties.get(file.getId, DriveSvlt.IMPORTANCE_PROP)
                    }.future.map(_.getValue)
                    val f_time = batcher {
                        drive.properties.get(file.getId, DriveSvlt.TIMESTAMP_PROP)
                    }.future.map(_.getValue)

                    val f_file_record = for {
                        imp <- f_imp
                        ts <- f_time
                    } yield {
                        val Array(base, half) = imp.split(" ").map(_.toInt)
                        DocRecord.inflate(base,
                                          half,
                                          ts.toLong,
                                          getCurrentTime)
                    }
                    f_file_record.map(rec => (file, rec))
                }
            }
        }.future.map { file_info_list =>
            file_info_list
            .sortBy { case (file, rec) => rec.current }
            .map { case (file, record) =>
                val base = ("title" -> file.getTitle) ~
                           ("parent" -> file.getParents.head.getId) ~
                           ("view_link" -> file.getAlternateLink):JValue
                base merge Extraction.decompose(record)
            }
        }

        batcher.execute

        val file_jsons = Await.result(f_file_jsons, 10.seconds)

        ("files" -> file_jsons)
    }
    */
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
}

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

import ireader.drive.{BaseDocRecord, DocRecord}
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
                    p => drive.getFile(p.getId)
                }
            } map { plist =>
                (folder, plist)
            }
        }.map { case (folder, parent_list) =>
            info(s"Generating parents")
            ("folder_title" -> folder.getTitle) ~
            ("parents" -> parent_list.map { f =>
                ("title" -> f.getTitle) ~
                ("id" -> f.getId)
            }):JValue
        }

        val f_children = drive.listFolderChildren(folder_id).flatMap { ids =>
            info(s"Sequencing children")
            Future.sequence {
                ids.map {
                    id => drive.getFile(id)
                }
            }
        }.map { children =>
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

        f_folders zip f_children map { case (folders, children) =>
            folders merge children
        }
    }

    post("/doc") {
        val parsed = parsedBody
        val sess = this.sess
        val JString(id) = parsed \ "id"

        val f_base_json = sess.drive.getFile(id).map { file =>
            ("title" -> file.getTitle) ~
            ("parent" -> file.getParents.head.getId) ~
            ("view_link" -> file.getAlternateLink):JValue
        }

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
            case JString("untrack") =>
                sess.props.remove(id).map(u => None)
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

    post("/queue") {
        val now = getCurrentTime
        val sess = this.sess

        val f_seq = sess.props.iterate.flatMap { iter =>
            Future.sequence {
                for {
                    (id, base_doc) <- iter
                    doc = DocRecord.inflate(base_doc, now)
                } yield for {
                    file <- sess.drive.getFile(id)
                } yield (file, doc)
            }
        }

        val f_filtered_seq = for {
            seq <- f_seq
            (trashed, not_trashed) = seq.partition {
                case (file, doc) => file.getLabels.getTrashed
            }
            u <- Future.sequence {
                trashed.map { case (file, doc) =>
                    info(s"Trashing ${file.getId}")
                    sess.props.remove(file.getId)
                }
            }
        } yield not_trashed

        val f_files_json = for {
            seq <- f_filtered_seq
            sorted = seq.toList.sortBy { case (file, doc) => doc.current }
        } yield for {
            (file, doc) <- sorted
        } yield {
            val base = ("id" -> file.getId) ~
            ("title" -> file.getTitle)
            base merge Extraction.decompose(doc)
        }

        for {
            files_json <- f_files_json
        } yield {
            ("files" -> files_json)
        }
    }
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
}

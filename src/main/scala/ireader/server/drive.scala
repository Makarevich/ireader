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

        val drive = session.drive.get
        val batcher1 = DriveBatcher(drive)
        val batcher2 = DriveBatcher(drive)

        val f_f_parent_list = batcher1.single {
            drive.files.get(folder_id)
        } map { folder =>
            batcher2.multiple {
                folder.getParents.map {
                    p => drive.files.get(p.getId)
                }
            } map { parents =>
                parents.map { p =>
                    ("title" -> p.getTitle) ~
                    ("id" -> p.getId)
                }
            }
        }

        val f_f_subfiles = batcher1.single {
            drive.children.list(folder_id).setQ("trashed = false")
        } map { children_list =>
            batcher2.multiple {
                children_list.getItems.map(ch => drive.files.get(ch.getId))
            } map { children =>
                val (folders, files) =
                    children.sortBy(_.getTitle)
                            .partition(_.getMimeType == DriveSvlt.FOLDER_MIME)

                ("folders" -> folders.map { f =>
                    ("title" -> f.getTitle) ~
                    ("id" -> f.getId)
                }) ~
                ("files" -> files.map { f =>
                    ("title" -> f.getTitle) ~
                    ("link" -> f.getAlternateLink)
                })
            }
        }

        batcher1.execute

        val Seq(f_parent_list, f_subfiles) = Await.result(Future.sequence {
            Seq(f_f_parent_list, f_f_subfiles)
        }, 30.seconds)

        batcher2.execute

        val Seq(parent_list, subfiles) = Await.result(Future.sequence {
            Seq(f_parent_list, f_subfiles)
        }, 30.seconds)

        subfiles ~
        ("parents" -> parent_list)
    }

    /*
    private def get_children(folder_id: String, batcher: DriveBatcher) = {
        val (folders_par, files_par) = {
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

        val folders_json = folders_par.map { f=>
            ("title" -> f.getTitle) ~
            ("id" -> f.getId)
        }.seq
        val files_json = files_par.map { f =>
            ("title" -> f.getTitle) ~
            ("link" -> f.getAlternateLink)
        }.seq

        ("folders" -> folders_json) ~
        ("files" -> files_json)
    }

    private def get_parents(folder_id: String, batcher: DriveBatcher) = {
        val folder = batcher.single { drive =>
            drive.files.get(folder_id)
        }
        val parents = batcher.multiple { drive =>
            folder.getParents.map { p => drive.files.get(p.getId) }
        }

        val parent_json_list = (parents :+ folder).par.map { p =>
            ("title" -> p.getTitle) ~
            ("id" -> p.getId)
        }.seq

        parent_json_list
    }
    */
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
}

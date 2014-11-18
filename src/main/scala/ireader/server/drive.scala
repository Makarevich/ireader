package ireader.server

import concurrent._
import concurrent.duration._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.services.drive.model.{About, File, Property}

import ireader.utils._


class DriveSvlt extends JsonSvlt {
    import collection.JavaConversions._
    import ExecutionContext.Implicits.global

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

    post("/doc") {
        val JString(id) = parsedBody \ "id"

        val drive = sess.drive.get
        val batcher = DriveBatcher(drive)


        lazy val prop_list_future = batcher {
            drive.properties.list(id)
        }.future

        val f_prop_opt: Future[Option[Property]] = parsedBody \ "action" match {
        case JString("init") | JString("update") =>
            val JInt(base) = parsedBody \ "base"
            val JInt(halflife) = parsedBody \ "half"
            val prop_string = Seq(base, halflife).mkString(" ")

            val prop = new Property
            prop.setKey(DriveSvlt.IMPORTANCE_PROP)
            prop.setValue(prop_string)

            batcher {
                drive.properties.update(id, prop.getKey, prop)
            }.map(x => Some(x)).future
        case JString("untrack") =>
            batcher {
                drive.properties.delete(id, DriveSvlt.IMPORTANCE_PROP)
            }
            Future.successful(None)
        case _ =>
            prop_list_future map { props =>
                props.getItems.find(_.getKey == DriveSvlt.IMPORTANCE_PROP)
            }
        }

        val f_ts_opt: Future[Option[Property]] = parsedBody \ "action" match {
        case JString("init") | JString("read") =>
            val prop = new Property
            prop.setKey(DriveSvlt.TIMESTAMP_PROP)
            prop.setValue(getCurrentTime.toString)

            batcher {
                drive.properties.update(id, prop.getKey, prop)
            }.map(x => Some(x)).future
        case JString("untrack") =>
            batcher {
                drive.properties.delete(id, DriveSvlt.TIMESTAMP_PROP)
            }
            Future.successful(None)
        case _ =>
            prop_list_future map { props =>
                props.getItems.find(_.getKey == DriveSvlt.TIMESTAMP_PROP)
            }
        }

        val f_base_json = batcher {
            drive.files.get(id)
        } map { file =>
            ("title" -> file.getTitle) ~
            ("parent" -> file.getParents.head.getId) ~
            ("view_link" -> file.getAlternateLink):JValue
        }

        val f_props_json = Future.sequence(Seq(f_prop_opt, f_ts_opt)).map { opts =>
            val Seq(prop_opt, ts_opt) = opts
            val json_opt = for {
                prop <- prop_opt
                ts <- ts_opt
            } yield {
                info(s"Extracting prop: ${prop.getValue}")
                val Array(base, half) = prop.getValue.split(" ").map(_.toInt)
                val doc = DocRecord.inflate(base,
                                            half,
                                            ts.getValue.toLong,
                                            getCurrentTime)
                info(s"Described: ${doc}")
                Extraction.decompose(doc)
            }
            json_opt.getOrElse(JNothing)
        }

        batcher.execute

        val Seq(base_json, props_json) = Await.result(Future.sequence{
            Seq(f_base_json.future, f_props_json)
        }, 10.seconds)

        base_json merge props_json
    }

    post("/folder") {
        val JString(folder_id: String) = parsedBody \ "folder_id"

        val drive = sess.drive.get
        val batcher = DriveBatcher(drive)

        val f_folders = batcher {
            drive.files.get(folder_id)
        }.flatMap { folder =>
            info(s"Parent count1 ${folder.getParents.size}")
            Future.sequence {
                info(s"Parent count2 ${folder.getParents.size}")
                folder.getParents.map {
                    p => batcher(drive.files.get(p.getId)).future
                }
            } map { plist =>
                (folder, plist)
            }
        }.future.map { case (folder, parent_list) =>
            //info(s"Generating parents")
            ("folder_title" -> folder.getTitle) ~
            ("parents" -> parent_list.map { f =>
                ("title" -> f.getTitle) ~
                ("id" -> f.getId)
            }):JValue
        }

        val f_children = batcher {
            drive.children.list(folder_id).setQ("trashed = false")
        }.flatMap { children_list =>
            Future.sequence {
                children_list.getItems.map {
                    ch => batcher(drive.files.get(ch.getId)).future
                }
            }
        }.future.map { children =>
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
        }, 10.seconds)

        folders merge children
    }
}

object DriveSvlt {
    private val FOLDER_MIME = "application/vnd.google-apps.folder"
    private val IMPORTANCE_PROP = "ireader_importance"
    private val TIMESTAMP_PROP = "ireader_timestamp"
}

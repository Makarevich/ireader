package ireader.drive.web

import collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.{File, FileList, ChildList}
import com.google.api.services.drive.model.{Property, PropertyList}
import com.google.api.services.drive.model.ParentReference

import ireader.drive.{IDriveApi, IPropsDB, BaseDocRecord}

class WebDriveApi(batcher: DriveBatcher)(implicit ec: ExecutionContext)
extends IDriveApi[File]
{
    private val drive = batcher.drive

    def getFile(fileId: String): Future[File] = {
        batcher.execute(drive.files.get(fileId)).mapTo[File]
    }

    def listFolderChildren(folderId: String): Future[List[String]] = {
        for {
            child_list <- batcher.execute{
                drive.children.list(folderId).setQ("trashed = false")
            }.mapTo[ChildList]
        } yield {
            info(s"Fecthed children!")
            child_list.getItems.map(_.getId).toList
        }
    }

    def insertNewFile(title: String,
                      mime: String,
                      parentId: String): Future[File] = {
        val file = (new File).setTitle(title).setMimeType(mime).setParents {
            val ref = new ParentReference
            ref.setId(parentId)
            List(ref)
        }

        batcher.execute(drive.files.insert(file)).mapTo[File]
    }
}

class WebFileProps(batcher: DriveBatcher)(implicit ec: ExecutionContext)
extends IPropsDB
{
    private val drive = batcher.drive

    private val TRACKING_KEY = "TRACKING";
    private val TRACKING_VALUE = "1";
    private val DOC_KEY = "BASE_DOC_INFO";
    private val TRACKING_QUERY =
        s"properties has { key='${TRACKING_KEY}' and value='${TRACKING_VALUE}' and visibility = 'PRIVATE' }";

    def get(key: String): Future[Option[BaseDocRecord]] = {
        for {
            prop_list <- batcher.execute {
                drive.properties.list(key)
            }.mapTo[PropertyList]
            props: List[Property] = prop_list.getItems.toList
            track_props = props.filter(elem => elem.getKey == TRACKING_KEY)
            doc_props = props.filter(elem => elem.getKey == DOC_KEY)
        } yield {
            assert(track_props.size == doc_props.size)

            if(track_props.isEmpty) None else {
                val List(track_prop) = track_props  // implicit assert(track_props.size == 1)
                val List(doc_prop) = doc_props
                assert(track_prop.getValue == TRACKING_VALUE)

                val Array(base, half, ts) = doc_prop.getValue.split(" ")
                Some(BaseDocRecord(base.toInt, half.toInt, ts.toLong))
            }
        }
    }

    def set(key: String, value: BaseDocRecord): Future[BaseDocRecord] = {
        val BaseDocRecord(base, half, ts) = value
        val doc_val = Seq(base, half, ts).map(_.toString).mkString(" ")
        val p1 = insert_prop(key, DOC_KEY, doc_val)
        val p2 = insert_prop(key, TRACKING_KEY, TRACKING_VALUE)
        for { u1 <- p1; u2 <- p2 } yield value
    }

    private def insert_prop(fileId: String,
                            prop_key: String,
                            prop_value: String): Future[Property] =
    {
        val prop = new Property
        prop.setKey(prop_key)
        prop.setValue(prop_value)
        batcher.execute(drive.properties.insert(fileId, prop)).mapTo[Property]
    }

    def remove(key: String): Future[Unit] = {
        for {
            u1 <- batcher.execute(drive.properties.delete(key, TRACKING_KEY))
            u2 <- batcher.execute(drive.properties.delete(key, DOC_KEY))
        } yield Unit
    }

    def iterate: Future[Iterator[(String, BaseDocRecord)]] = {
        for {
            file_list <- batcher.execute {
                drive.files.list.setQ(TRACKING_QUERY)
            }.mapTo[FileList]
        } yield for {
            file <- file_list.getItems.toIterator
        } yield {
            val fileId = file.getId
            val props = file.getProperties.toList
            val List(doc_prop) = props.filter(prop => prop.getKey == DOC_KEY)

            val Array(base, half, ts) = doc_prop.getValue.split(" ")
            val record = BaseDocRecord(base.toInt, half.toInt, ts.toLong)
            (fileId, record)
        }
    }
}

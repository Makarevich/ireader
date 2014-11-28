package ireader.drive.web

import collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.ParentReference

import ireader.drive.{IDriveApi, IDriveIOApi}
import ireader.drive.FutureProxy

class WebDriveApi(drive: Drive) extends IDriveApi[File] {
    private val batcher = DriveBatcher(drive)

    def getFile(fileId: String): FutureProxy[File] = {
        batcher {
            drive.files.get(fileId)
        }
    }

    def listFolderChildren(folderId: String): FutureProxy[List[String]] = {
        val fp = batcher {
            drive.children.list(folderId).setQ("trashed = false")
        } map { child_list =>
            info(s"Fecthed children!")
            child_list.getItems.map(_.getId).toList
        }
        fp//.rewrap
    }

    def insertNewFile(title: String,
                      mime: String,
                      parentId: String): FutureProxy[File] = {
        val file = (new File).setTitle(title).setMimeType(mime).setParents {
            val ref = new ParentReference
            ref.setId(parentId)
            List(ref)
        }

        batcher {
            drive.files.insert(file)
        }
    }

    def execute: Unit = {
        batcher.execute
    }
}

class WebDriveIOApi(drive: Drive) extends IDriveIOApi {
    def getFileContent(fileId: String): Future[String] = Future {
        info("getFileContent")
        import io.Source
        import com.google.api.client.http.GenericUrl

        val file = drive.files.get(fileId).execute

        val response = drive.getRequestFactory.buildGetRequest(
                new GenericUrl(file.getDownloadUrl)).execute

        Source.fromInputStream(response.getContent).getLines.mkString("\n")
    }

    def saveFileContent(fileId: String, content: String): Future[Unit] = Future {
        info("saveFileContent")
        import java.io.ByteArrayInputStream
        import java.nio.charset.StandardCharsets
        import com.google.api.client.http.InputStreamContent

        val file = drive.files.get(fileId).execute

        val stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        val stream_content = new InputStreamContent("text/plain", stream)
        drive.files.update(fileId, file, stream_content).execute
    }
}


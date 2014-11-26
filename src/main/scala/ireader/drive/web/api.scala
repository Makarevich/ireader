package ireader.drive.web

import collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

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

    def execute: Unit = {
        batcher.execute
    }
}

abstract class WebDriveIOApi(drive: Drive) extends IDriveIOApi {
}


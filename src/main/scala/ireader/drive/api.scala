package ireader.drive

import concurrent.{Future,ExecutionContext}
import io.Source

// NOTE: FileData here may represent different actual classes in each trait

trait IDriveApi[FileData] {
    def getFile(fileId: String): FutureProxy[FileData]      // TODO: switch to custom data structure??
    def listFolderChildren(folderId: String): FutureProxy[List[String]]
    def execute: Unit
}

trait IFileProps[FileData] {
    def get(key: String): Future[Option[FileData]]
    def set(key: String, value: FileData): Future[FileData]
    def remove(key: String): Future[Unit]
    def iterate: Future[Iterator[(String, FileData)]]
}

trait IDriveIOApi {
    def getFileContent(fileId: String): FutureProxy[Source]
    def saveFileContent(fileId: String, content: String): Future[Unit]
}


package ireader.drive

import concurrent.{Future,ExecutionContext}

// NOTE: FileData here may represent different actual classes in each trait

trait IDriveApi[FileData] {
    def getFile(fileId: String): FutureProxy[FileData]      // TODO: switch to custom data structure
    def listFolderChildren(folderId: String): FutureProxy[List[String]]
    def execute: Unit
}

trait IFileProps[FileData] {
    def get(key: String): Future[Option[FileData]]
    def set(key: String, value: FileData): Future[FileData]
}

trait IDriveApiIO[FileData] {
    def getFileContent(fileId: String): FutureProxy[FileData]
    def saveFileContent(fileId: String, data: FileData): Future[Unit]
}


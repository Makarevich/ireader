package ireader.drive

import scala.concurrent.{Future,ExecutionContext}

import com.google.api.services.drive.model.File

trait IDriveApi {
    def getFile(fileId: String): FutureProxy[File]
    def listFolderChildren(folderId: String): FutureProxy[List[String]]
    def execute: Unit
}

trait IDriveApiIO[FileData] {
    def getFileContent(fileId: String): FutureProxy[FileData]
    def saveFileContent(fileId: String, data: FileData): Future[Unit]
}

class FutureProxy[+T] (val future: Future[T])
                      (cb: (Future[_]) => Unit)
{
    private def ready[S](f: Future[S]): Future[S] = {
        cb(f)
        f
    }

    private def wrap[S](f: Future[S]): FutureProxy[S] = {
        new FutureProxy(f)(cb)
    }

    def map[S](f: (T) => S)(implicit executor: ExecutionContext): FutureProxy[S] = {
        wrap(ready(future.map(f)))
    }

    def flatMap[S](f: (T) => Future[S])(implicit executor: ExecutionContext): FutureProxy[S] = {
        wrap(ready(future.map(f)).flatMap(x => x))
    }

    // def zip = ???
}

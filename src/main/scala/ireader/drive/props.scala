package ireader.drive

import concurrent.{Future,ExecutionContext}
import collection.mutable.{Map => MutableMap}

class FilePropsDatabase extends IFileProps[BaseDocRecord] {
    private val data = MutableMap.empty[String, BaseDocRecord]

    def get(key: String): Future[Option[BaseDocRecord]] =
    synchronized {
        Future.successful(data.get(key))
    }

    def set(key: String, value: BaseDocRecord): Future[BaseDocRecord] =
    synchronized {
        data.update(key, value)
        Future.successful(value)
    }
}


package ireader.drive

import concurrent.Future
import collection.mutable.{Map => MutableMap}

trait IPropsDB extends IFileProps[BaseDocRecord]

trait IInMemPropsDB extends IPropsDB {
    private val cache = MutableMap.empty[String, BaseDocRecord]

    def get(key: String): Future[Option[BaseDocRecord]] = synchronized {
        Future.successful(cache.get(key))
    }

    def set(key: String, value: BaseDocRecord): Future[BaseDocRecord] = synchronized {
        cache.update(key, value)
        Future.successful(value)
    }

    def remove(key: String): Future[Unit] = synchronized {
        cache.remove(key)
        Future.successful(Unit)
    }

    def iterate: Future[Iterator[(String, BaseDocRecord)]] = synchronized {
        Future.successful(cache.clone.toIterator)
    }

    //def iterateKeys:
}


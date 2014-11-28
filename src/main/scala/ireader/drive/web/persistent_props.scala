package ireader.drive.web

import concurrent.{Future,ExecutionContext}

import ireader.drive.BaseDocRecord
import ireader.drive.IDriveIOApi
import ireader.drive.IInMemPropsDB

class PersistentPropsDB(drive_io: IDriveIOApi,
                        file_locator: DBFileLocator)
                       (implicit ectxt: ExecutionContext)
extends IInMemPropsDB
{
    override def get(key: String): Future[Option[BaseDocRecord]] = {
        deserizalize.flatMap(u => super.get(key))
    }

    override def set(key: String, value: BaseDocRecord): Future[BaseDocRecord] = {
        this.flush(super.set(key,value))
    }

    override def remove(key: String): Future[Unit] = {
        this.flush(super.remove(key))
    }

    override def iterate = {
        deserizalize.flatMap(u => super.iterate)
    }

    //////

    private def flush[T](result: Future[T]): Future[T] = {
        for {
            id <- fileId
            r <- result
            content <- serialize
            u <- drive_io.saveFileContent(id, content)
        } yield r
    }

    private def serialize: Future[String] = synchronized {
        for {
            u <- deserizalize
            it <- iterate
        } yield {
            val lines = for {
                pair <- it
                (key, doc) = pair
                BaseDocRecord(base, half, ts) = doc
            } yield {
                Seq(key, base, half, ts).map(_.toString).mkString(" ")
            }
            lines.mkString("\n")
        }
    }

    private lazy val deserizalize: Future[Unit] = synchronized {
        for {
            id <- fileId
            text <- drive_io.getFileContent(id)
            r <- Future.sequence {
                val it = for {
                    line <- text.split('\n')
                    if !line.isEmpty
                    Array(key, base, half, ts) = line.split(" ")
                    doc = BaseDocRecord(base.toInt, half.toInt, ts.toLong)
                } yield {
                    super.set(key, doc)
                }
                it.toList
            }
        } yield {}
    }

    private[this] lazy val fileId: Future[String] = file_locator.locate
}

class DBFileLocator(api: WebDriveApi)
                   (implicit ectxt: ExecutionContext)
{
    def locate: Future[String] = {
        info("Locating db storage")
        val f_proxy = {
            api.listFolderChildren(DBFileLocator.LOOKUP_FOLDER).flatMap { names =>
                Future.sequence {
                    names.map { name => api.getFile(name).future }
                }
            } flatMap { children =>
                val f_id_opt: Option[Future[String]] = for {
                    child <- children.find { child =>
                        child.getTitle == DBFileLocator.EXPECTED_TITLE
                    }
                } yield Future.successful(child.getId)
                f_id_opt.getOrElse(get_new_file_id)
            }
        }
        info("Staring locator's api")
        api.execute
        info("Finished locator's api")
        f_proxy.future
    }

    private def get_new_file_id: Future[String] = {
        info("Inserting empty storage file")
        val file = api.insertNewFile(DBFileLocator.EXPECTED_TITLE,
                                     DBFileLocator.STORAGE_MIME_TYPE,
                                     DBFileLocator.LOOKUP_FOLDER)
        val result = file.map(_.getId).future
        result
    }
}

object DBFileLocator {
    private val LOOKUP_FOLDER = "root"
    private val EXPECTED_TITLE = "PROPS"
    private val STORAGE_MIME_TYPE = "text/plain"
}


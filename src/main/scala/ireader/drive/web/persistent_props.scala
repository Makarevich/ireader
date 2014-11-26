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
        for {
            u <- deserizalize
            result <- super.get(key)
        } yield result
    }

    override def set(key: String, value: BaseDocRecord): Future[BaseDocRecord] = {
        this.flush(super.set(key,value))
    }

    override def remove(key: String): Future[Unit] = {
        this.flush(super.remove(key))
    }

    //////

    private def flush[T](result: Future[T]): Future[T] = {
        for {
            id <- fileId
            content <- serialize
            u <- drive_io.saveFileContent(id, content)
            r <- result
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
        val f_proxy = for {
            names <- api.listFolderChildren(DBFileLocator.LOOKUP_FOLDER)
            children <- Future.sequence {
                names.map { name => api.getFile(name).future }
            }
        } yield {
            val file_opt = children.find { child =>
                child.getTitle == DBFileLocator.EXPECTED_TITLE
            }
            file_opt.map(_.getId).get//.getOrElse(get_new_file_id)
        }
        f_proxy.future
    }

    //private def get_new_file_id: Future[String]
}

object DBFileLocator {
    private val LOOKUP_FOLDER = "root"
    private val EXPECTED_TITLE = "PROPS"
}


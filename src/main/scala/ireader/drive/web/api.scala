package ireader.drive.web

import collection.JavaConversions._
import util.Success
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.{ActorSystem, Props, ActorRef, ActorNotFound}
import akka.util.Timeout

import com.google.api.services.drive.{Drive, DriveRequest}
import com.google.api.services.drive.model.{File, ChildList}
import com.google.api.services.drive.model.ParentReference

import ireader.drive.{IDriveApi, IDriveIOApi}

class WebDriveApi(drive: Drive, actor_system: ActorSystem)
extends IDriveApi[File]
{
    import actor_system.dispatcher
    private implicit val timeout: Timeout = 10.seconds

    private val batcher: Future[ActorRef] = {
        val sel = actor_system.actorSelection("/user/batcher")
        sel.resolveOne().recover {
        case e: ActorNotFound =>
            info(e.getMessage)
            actor_system.actorOf(Props[BatchingActor], "batcher")
        }
    } andThen {
    case Success(actor) => actor ! drive
    }

    private def ask[ResType](msg: DriveRequest[_]): Future[Any] =
    {
        for {
            b <- batcher
            ff <- akka.pattern.ask(b, msg).mapTo[Future[Any]]
            f <- ff
        } yield f
    }
    def getFile(fileId: String): Future[File] = {
        ask(drive.files.get(fileId)).mapTo[File]
    }

    def listFolderChildren(folderId: String): Future[List[String]] = {
        for {
            child_list <- ask{
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

        ask(drive.files.insert(file)).mapTo[File]
    }
}

class WebDriveIOApi(drive: Drive)(implicit ec: ExecutionContext)
        extends IDriveIOApi
{
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


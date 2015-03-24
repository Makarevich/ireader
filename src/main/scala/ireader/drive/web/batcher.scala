package ireader.drive.web

import annotation.tailrec
import util.Success
import concurrent._
import concurrent.duration._
import akka.actor.Actor
import akka.event.{Logging, LoggingReceive}
import akka.actor.{ActorSystem, Props, ActorRef, ActorNotFound}
import akka.util.Timeout

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.{Drive, DriveRequest}

class DriveBatcher(actor_system: ActorSystem, val drive: Drive)
{
    import actor_system.dispatcher

    def execute(msg: DriveRequest[_]): Future[Any] =
    {
        for {
            b <- actor_f
            ff <- akka.pattern.ask(b, msg).mapTo[Future[Any]]
            f <- ff
        } yield f
    }


    private implicit val timeout: Timeout = 10.seconds

    private val actor_f: Future[ActorRef] = {
        val sel = actor_system.actorSelection("/user/batcher")
        sel.resolveOne().recover {
        case e: ActorNotFound =>
            info(e.getMessage)
            actor_system.actorOf(Props[BatchingActor], "batcher")
        }
    } andThen {
    case Success(actor) => actor ! drive
    }

}

class BatchingActor extends Actor {
    object TICK

    val log = Logging(context.system, this)
    var drive: Drive = null
    var batch: BatchRequest = null

    val tick = context.system.scheduler.schedule(
        100.millis, 100.millis, self, TICK)(
        context.dispatcher)


    private def sched_request[T](req: DriveRequest[T]): Future[T] = {
        val promise = Promise[T]

        val cb = new JsonBatchCallback[T] {
            def onSuccess(f: T, headers: HttpHeaders) {
                promise.success(f)
            }
            def onFailure(err: GoogleJsonError, headers: HttpHeaders): Unit = {
                promise.failure(new RuntimeException(err.getMessage))       // TODO: imp more meaningful exception message
            }
        }
        req.queue(batch, cb)
        promise.future
    }

    override def preStart = {
        log.info("Bacther started")
    }

    override def postStop() = {
        log.info("Bacther stopped")
        tick.cancel()
    }

    def receive = LoggingReceive {
        drive_updater andThen {
        case _ => context.become(drive_updater orElse request_receiver)
        }
    }

    def drive_updater: Receive = {
    case new_drive: Drive =>
        log.info("Swapping drive")
        this.drive = new_drive
        this.batch = this.drive.batch
    }

    def request_receiver: Receive = {
    case req: DriveRequest[_] =>
        sender() ! sched_request(req)
    case TICK =>
        if (batch.size > 0) {
            log.info(s"Executing batch of size ${batch.size}")
            this.batch.execute
            this.batch = this.drive.batch
        }
    }
}


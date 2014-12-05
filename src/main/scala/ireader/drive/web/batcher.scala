package ireader.drive.web

import annotation.tailrec
import concurrent._
import concurrent.duration._
import akka.actor.Actor
import akka.event.{Logging, LoggingReceive}

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest

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

/*
class DriveBatcher(drive: Drive) {
    private class State {
        var batch = drive.batch
        var listener: List[Future[Any]] = Nil
    }
    private var state = new State

    def addListener(f: Future[Any]): Unit = synchronized {
        state.listener = f :: state.listener
    }

    def apply[T](req: DriveRequest[T])
                (implicit executor: ExecutionContext): FutureProxy[T] =
    synchronized {
        val promise = Promise[T]
        val cb = new JsonBatchCallback[T] {
            def onSuccess(f: T, headers: HttpHeaders) {
                promise.success(f)
            }
            def onFailure(err: GoogleJsonError, headers: HttpHeaders): Unit = {
                promise.failure(new RuntimeException(err.getMessage))       // TODO: imp more meaningful exception message
            }
        }
        req.queue(state.batch, cb)

        val fp = new FutureProxy(promise.future)(this.addListener)
        addListener(fp.future)
        fp
    }

    @tailrec
    final def execute(implicit executor: ExecutionContext) {
        val old_state = synchronized {
            val oldie = this.state
            this.state = new State
            oldie
        }

        val wait_for = old_state.listener
        val old_batch = old_state.batch

        if (old_batch.size > 0) {
            info(s"Executing batcher with ${old_batch.size} requests")
            old_batch.execute
        }

        if(!wait_for.isEmpty) {
            // val waits = wait_for.map(_.hashCode.toString).mkString(",")
            // info(s"Waiting for ${wait_for.length} events: ${waits}")
            info(s"Waiting for ${wait_for.length} events")
            Await.ready(Future.sequence(wait_for), 10.seconds)
            execute
        } else {
            info(s"Batcher finished")
        }
    }
}

object DriveBatcher {
    def apply(d: Drive) = new DriveBatcher(d)
}
*/

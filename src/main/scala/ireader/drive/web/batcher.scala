package ireader.drive.web

import annotation.tailrec
import concurrent._
import concurrent.duration._

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest

import ireader.drive.FutureProxy

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

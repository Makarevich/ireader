package ireader.utils

import annotation.tailrec
import concurrent._
import concurrent.duration._

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest

class DriveBatcher(drive: Drive) {
    private var batch = drive.batch
    private var listener: List[Future[Any]] = Nil

    def addListener(f: Future[Any]): Unit = synchronized {
        listener = f :: listener
    }

    def apply[T](req: DriveRequest[T]): FutureProxy[T] = single(req)
    def single[T](req: DriveRequest[T]): FutureProxy[T] = synchronized {
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
        new FutureProxy(promise.future, this)
    }

    //def multiple[T, CC[_] <: TraversableLike[DriveRequest[T], _]]
    //        (reqs: CC[DriveRequest[T]])
    //        (implicit ectxt: ExecutionContext) =
    //    Future.sequence(reqs.map((r: DriveRequest[T]) => single(r)))

    @tailrec
    final def execute(implicit executor: ExecutionContext) {
        var (wait_for, old_batch) = synchronized {
            var wait_for = listener
            var old_batch = batch

            listener = Nil
            batch = drive.batch

            (wait_for, old_batch)
        }

        if (old_batch.size > 0) {
            info(s"Executing batcher x ${old_batch.size}")
            old_batch.execute

            if(!wait_for.isEmpty) {
                val waits = wait_for.map(_.toString).mkString(",")
                info(s"Waiting for ${wait_for.length} events: ${waits}")
                Await.ready(Future.sequence(wait_for), 10.seconds)
                execute
            }
        }
    }
}

object DriveBatcher {
    def apply(d: Drive) = new DriveBatcher(d)
}

class FutureProxy[+T] (val future: Future[T], batcher: DriveBatcher) {
    private def addListener[S](f: Future[S]): Future[S] = {
        batcher.addListener(f)
        f
    }

    private def wrap[S](f: Future[S]): FutureProxy[S] = {
        info(s"Wrapping ${f}")
        new FutureProxy(f, batcher)
    }

    def map[S](f: (T) => S)(implicit executor: ExecutionContext): FutureProxy[S] = {
        wrap(addListener(future.map(f)))
    }

    def flatMap[S](f: (T) => Future[S])(implicit executor: ExecutionContext): FutureProxy[S] = {
        wrap(addListener(future.map(f)).flatMap(x => x))
    }
}

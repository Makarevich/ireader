package ireader.utils

import collection.TraversableLike
import collection.generic.CanBuildFrom
import concurrent.{Future, Promise,ExecutionContext}

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest

class DriveBatcher(drive: Drive) {
    private val batch = drive.batch

    def single[T](req: DriveRequest[T]): Future[T] = batch.synchronized {
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

    def multiple[From <: TraversableLike[DriveRequest[_], From]]
            (reqs: From)
            (implicit ectxt: ExecutionContext) =
        Future.sequence(reqs.map(r => single(r)))

    def execute: Unit = batch.synchronized {
        batch.execute
    }
}

object DriveBatcher {
    def apply(d: Drive) = new DriveBatcher(d)
}

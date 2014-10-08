package ireader.utils

import collection.TraversableLike
import collection.generic.CanBuildFrom

import com.google.api.client.http.HttpHeaders
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest

class DriveBatcher(drive: Drive) {
    def single[T](req: (Drive) => DriveRequest[T]) = req(drive).execute()

    def multiple[T, From <: TraversableLike[DriveRequest[T], From], To]
            (f: (Drive) => From with TraversableLike[DriveRequest[T], From])
            (implicit cbf: CanBuildFrom[From, T, To]): To =
    {
        val reqs = f(drive)
        val result_builder = cbf(reqs)
        if (!reqs.isEmpty) {
            val cb = new JsonBatchCallback[T] {
                def onSuccess(f: T, headers: HttpHeaders) {
                    result_builder += f
                }
                def onFailure(err: GoogleJsonError, headers: HttpHeaders): Unit = ???
            }

            val batch = drive.batch
            reqs.foreach(_.queue(batch, cb))
            batch.execute
        }
        result_builder.result
    }
}

object DriveBatcher {
    def apply(d: Drive) = new DriveBatcher(d)
}

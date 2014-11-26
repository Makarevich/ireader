package ireader.drive

import scala.concurrent.{Future,ExecutionContext}
import com.google.api.services.drive.model.File


class FutureProxy[+T] (val future: Future[T])
                      (cb: (Future[_]) => Unit)
                      (implicit executor: ExecutionContext)
{
    // private[this] var wait_fors = List.empty[Future[Any]]

    /*
    val future = orig_future.andThen {
        // push the collected wait-fors only when the underlying future is ready
        case _ => synchronized {
            wait_fors.foreach(cb)
        }
    }
    */

    private def ready[S](f: Future[S]): Future[S] = {
        //wait_fors = f :: wait_fors
        cb(f)
        f
    }

    private def wrap[S](f: Future[S]): FutureProxy[S] = {
        new FutureProxy(f)(cb)
    }

    // def rewrap = new FutureProxy(orig_future)(cb)

    def map[S](f: (T) => S): FutureProxy[S] = {
        wrap(ready(future.map(f)))
    }

    def flatMap[S](f: (T) => Future[S]): FutureProxy[S] = {
        wrap(ready(future.map(f)).flatMap(x => x))
    }

    // def zip = ???
}

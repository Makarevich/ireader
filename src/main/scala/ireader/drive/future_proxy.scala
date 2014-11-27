package ireader.drive

import scala.concurrent.{Future,ExecutionContext}


class FutureProxy[+T] (orig_future: Future[T])
                      (cb: (Future[_]) => Unit)
                      (implicit executor: ExecutionContext)
{
    private[this] var wait_fors = List.empty[Future[Any]]

    val future: Future[T] = orig_future.andThen {
        // push the collected wait-fors only when the underlying future is ready
        case _ => synchronized {
            // val waits = wait_fors.map(_.hashCode.toString).mkString(",")
            // info(s"Pushing futures ${future.hashCode}: ${waits}")
            wait_fors.foreach(cb)
        }
    }

    private def wait[S](f: Future[S]): Future[S] = {
        synchronized {
            wait_fors = f :: wait_fors
        }
        f
    }

    private def wrap[S](f: Future[S]): FutureProxy[S] = {
        new FutureProxy(f)(cb)
    }

    def map[S](f: (T) => S): FutureProxy[S] = {
        val result = wrap(future.map(f))
        wait(result.future)
        // info(s"Mapped ${future.hashCode} ${result.future.hashCode}")
        result
    }

    def flatMap[S](f: (T) => Future[S]): FutureProxy[S] = {
        val result = wrap(wait(future.map(f)).flatMap(x => x))
        // info(s"Flatmapped ${future.hashCode} ${result.future.hashCode}")
        result
    }

    // def zip = ???
    // def andThen = ???
}

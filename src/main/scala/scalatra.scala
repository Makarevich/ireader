import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import ireader.server._
import org.slf4j.{LoggerFactory,Logger}
//import org.apache.log4j.Level


class ScalatraBootstrap extends LifeCycle {
    override def init(context: ServletContext) {
        info("Bootsrtapping server...")

        //LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO)

        context mount (new StaticSvlt, "/")
        context mount (new StartTimeSvlt, "/start-time")
    }
}

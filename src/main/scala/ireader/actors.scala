package ireader

import scala.collection.JavaConversions._

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object ActorSystemContainer {
    lazy val logger = LoggerFactory.getLogger("ActorSystemContainer")
    lazy val system: ActorSystem = {
        val config = ConfigFactory.parseString {
            """akka {
              | loggers = ["akka.event.slf4j.Slf4jLogger"]
              | loglevel = "DEBUG"
              | log-dead-letters = "on"
              | actor {
              |   debug {
              |     receive = on
              |     lifecycle = on
              |   }
              | }
              |}""".stripMargin
              //| log-config-on-start = "on"
        }

        logger.info(s"Spawning actor system")
        val system = ActorSystem("default", config)
        system
    }
}



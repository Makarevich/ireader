package ireader

import java.util.Date
import java.text.DateFormat
import org.slf4j.LoggerFactory


package object utils {
    lazy val logger = LoggerFactory.getLogger("utils")
    def info(s: String) = logger.info(s)

    def getCurrentTime = System.currentTimeMillis / 1000
}

package object server {
    lazy val logger = LoggerFactory.getLogger("server")
    def info(s: String) = logger.info(s)

    lazy val started_on: String = {
        val fmt = DateFormat.getDateTimeInstance
        fmt.format(new Date())
    }
}


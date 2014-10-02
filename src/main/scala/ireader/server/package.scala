package ireader

import java.util.Date
import java.text.DateFormat
import org.slf4j.LoggerFactory


package object server {
    lazy val logger = LoggerFactory.getLogger("server")

    def info(s: String) = logger.info(s)

    lazy val started_on: String = {
        val fmt = DateFormat.getDateTimeInstance
        fmt.format(new Date())
    }
}


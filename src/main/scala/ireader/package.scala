package ireader

import java.util.Date
import java.text.DateFormat

import ireader.utils.InfoLogger


package object drive {
    val info = new InfoLogger("drive")
}

package drive {
    package object web {
        val info = new InfoLogger("drive.web")
    }
}

package object server {
    val info = new InfoLogger("server")

    lazy val started_on: String = {
        val fmt = DateFormat.getDateTimeInstance
        fmt.format(new Date())
    }
}


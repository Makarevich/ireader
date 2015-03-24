package ireader

package object utils {
    //lazy val logger = LoggerFactory.getLogger("utils")
    //def info(s: String) = logger.info(s)

    def getCurrentTime = System.currentTimeMillis / 1000
}

package utils {

import org.slf4j.LoggerFactory

class InfoLogger(name: String) {
    private lazy val logger = LoggerFactory.getLogger(name)

    def apply(s: String) = logger.info(s)
}

} // package utils


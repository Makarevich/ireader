package ireader.drive.web

import com.redis.RedisClient


trait IRedisCache {
    def get: Option[String]
    def set(value: String): Unit
}

class RedisTokenCache extends IRedisCache {
    def get: Option[String] = {
        val result = client_opt.flatMap(_.get(RedisTokenCache.REDIS_KEY))
        result match {
            case Some(_) => info("Fetched token from redis")
            case None => info("Fetched NO token from redis")
        }
        result
    }

    def set(value: String) {
        info("Setting to redis")
        client_opt.foreach(_.set(RedisTokenCache.REDIS_KEY, value))
    }

    private def client_opt = {
        try {
            Some(new RedisClient)
        } catch {
            case e: java.lang.RuntimeException => None
        }
    }
}

object RedisTokenCache {
    private val REDIS_KEY = "ireader_access_token"
}


package ireader.drive

trait ITokenContainer {
    def token: String
}

trait ITokenContainerFactory {
    def make: ITokenContainer
    def make(new_value: String): ITokenContainer
}

//////

class RedisTokenContainer(cache: IRedisCache) extends ITokenContainer {
    lazy val token: String = {
        cache.get match {
        case Some(token) =>
            info("Fetched access token")
            token
        case None =>
            throw new RuntimeException("Couldn't fetch access token")
        }
    }
}

class StringTokenContainer(t: String) extends ITokenContainer {
    val token: String = t
}


class PersistanceTokenContainerFactory(cache: IRedisCache) extends ITokenContainerFactory {
    def make: ITokenContainer = {
        new RedisTokenContainer(cache)
    }

    def make(new_value: String): ITokenContainer = {
        cache.set(new_value)
        new StringTokenContainer(new_value)
    }
}


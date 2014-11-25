package ireader.server

package linker

import ireader.drive.web

object AuthFlow extends web.AuthFlowProvider

object TokenContainerFactory
    extends web.PersistanceTokenContainerFactory(RedisCache)

object GoogleDriveFactory extends web.GoogleAuthDriveFactory(AuthFlow)

object RedisCache extends web.RedisTokenCache


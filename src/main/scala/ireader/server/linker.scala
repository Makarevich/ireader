package ireader.server

package linker

import ireader.drive.web

object AuthFlow extends web.AuthFlowProvider

object RedisCache extends web.RedisTokenCache

object TokenContainerFactory
    extends web.PersistanceTokenContainerFactory(RedisCache)

object GoogleDriveFactory extends web.GoogleAuthDriveFactory(AuthFlow)

object SessionStateFactory extends web.SessionStateFactory(
        GoogleDriveFactory, ireader.ActorSystemContainer.system
)

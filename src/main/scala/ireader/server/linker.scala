package ireader.server

package linker

import ireader.drive

object AuthFlow extends drive.AuthFlowProvider

object TokenContainerFactory
    extends drive.PersistanceTokenContainerFactory(RedisCache)

object GoogleDriveFactory extends drive.GoogleAuthDriveFactory(AuthFlow)

object RedisCache extends drive.RedisTokenCache


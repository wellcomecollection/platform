package uk.ac.wellcome.platform.matcher.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.matcher.lockable.DynamoLockingServiceConfig

object DynamoLockingServiceConfigModule extends TwitterModule{

  private val lockTableName = flag[String](
    "aws.dynamo.locking.service.lockTableName",
    "",
    "Dynamo table name to use for locking")

  @Provides
  def provideDynamoLockingServiceConfig(injector: Injector) = {
        DynamoLockingServiceConfig(
          lockTableName()
        )
  }
}

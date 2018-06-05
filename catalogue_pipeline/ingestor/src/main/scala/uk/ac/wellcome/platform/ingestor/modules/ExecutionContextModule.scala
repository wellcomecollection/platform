package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton
import uk.ac.wellcome.platform.ingestor.GlobalExecutionContext

import scala.concurrent.ExecutionContext

object ExecutionContextModule extends TwitterModule {
  @Provides
  @Singleton
  def provideExecutionContext(injector: Injector): ExecutionContext =
    GlobalExecutionContext.context
}

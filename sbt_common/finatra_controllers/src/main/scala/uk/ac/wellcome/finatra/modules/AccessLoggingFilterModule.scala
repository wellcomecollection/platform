package uk.ac.wellcome.finatra.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.filters.PlatformLoggingFilter

object AccessLoggingFilterModule extends TwitterModule {

  @Singleton
  @Provides
  def providesAccessLoggingFilter(injector: Injector): AccessLoggingFilter[Request] = {
    val logFormatter = injector.instance[LogFormatter[Request, Response]]
    new PlatformLoggingFilter(logFormatter = logFormatter)
  }
}

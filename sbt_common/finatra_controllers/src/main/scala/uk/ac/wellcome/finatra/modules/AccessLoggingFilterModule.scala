package uk.ac.wellcome.finatra.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.filters.PlatformLoggingFilter

/** Provides the [[PlatformLoggingFilter]] which doesn't log ALB healthchecks.
  *
  * To see where this injection is used:
  *
  *   - Note that all of our [[com.twitter.finatra.http.HttpServer]] instances
  *     configure [[com.twitter.finatra.http.filters.CommonFilters]].
  *   - The second parameter to [[com.twitter.finatra.http.filters.CommonFilters]]
  *     is an instance of [[AccessLoggingFilter]].
  *   - Normally that filter is provided by some builtin Finatra module --
  *     by using this module, we can override the default filter.
  *
  */
object AccessLoggingFilterModule extends TwitterModule {

  @Singleton
  @Provides
  def providesAccessLoggingFilter(injector: Injector): AccessLoggingFilter[Request] = {
    val logFormatter = injector.instance[LogFormatter[Request, Response]]
    new PlatformLoggingFilter(logFormatter = logFormatter)
  }
}

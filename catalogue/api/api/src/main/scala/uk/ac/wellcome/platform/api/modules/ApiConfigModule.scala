package uk.ac.wellcome.platform.api.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.api.models.ApiConfig

object ApiConfigModule extends TwitterModule {
  private val host =
    flag(name = "api.host", default = "localhost:8888", help = "API hostname")
  private val scheme =
    flag(name = "api.scheme", default = "https", help = "API protocol scheme")
  private val defaultPageSize =
    flag(name = "api.pageSize", default = 10, help = "API default page size")

  private final val apiName =
    flag(name = "api.name", default = "catalogue", help = "API name path part")
  private val pathPrefix = flag[String](
    name = "api.prefix",
    default = "/" + apiName(),
    help = "API path prefix")

  private val contextSuffix = flag(
    name = "api.context.suffix",
    default = "/context.json",
    help = "Relative API JSON-LD context")

  @Provides
  @Singleton
  def providesApiConfig(): ApiConfig =
    ApiConfig(
      host = host(),
      scheme = scheme(),
      defaultPageSize = defaultPageSize(),
      pathPrefix = pathPrefix(),
      contextSuffix = contextSuffix()
    )
}

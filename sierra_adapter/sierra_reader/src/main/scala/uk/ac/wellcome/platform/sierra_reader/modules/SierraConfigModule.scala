package uk.ac.wellcome.platform.sierra_reader.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.sierra_reader.models.SierraConfig
import uk.ac.wellcome.platform.sierra_reader.models.SierraResourceTypes.{
  bibs,
  items
}

object SierraConfigModule extends TwitterModule {
  private val resourceTypeFlag =
    flag[String]("reader.resourceType", "Sierra resource type")
  private val apiUrl = flag[String]("sierra.apiUrl", "", "Sierra API url")
  private val oauthKey =
    flag[String]("sierra.oauthKey", "", "Sierra API oauth key")
  private val oauthSec =
    flag[String]("sierra.oauthSecret", "", "Sierra API oauth secret")
  private val fields = flag[String](
    "sierra.fields",
    "",
    "List of fields to include in the Sierra API response")

  @Singleton
  @Provides
  def providesSierraConfig(): SierraConfig = {
    val resourceType = resourceTypeFlag() match {
      case s: String if s == bibs.toString => bibs
      case s: String if s == items.toString => items
      case s: String =>
        throw new IllegalArgumentException(
          s"$s is not a valid Sierra resource type")
    }

    SierraConfig(
      resourceType = resourceType,
      apiUrl = apiUrl(),
      oauthKey = oauthKey(),
      oauthSec = oauthSec(),
      fields = fields()
    )
  }
}

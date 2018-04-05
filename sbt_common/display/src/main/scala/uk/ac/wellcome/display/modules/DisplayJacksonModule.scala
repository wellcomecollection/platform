package uk.ac.wellcome.display.modules

import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import uk.ac.wellcome.display.models.WorksIncludesDeserializerModule

object DisplayJacksonModule extends FinatraJacksonModule {
  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy
  override val additionalJacksonModules = Seq(
    new WorksIncludesDeserializerModule
  )
}

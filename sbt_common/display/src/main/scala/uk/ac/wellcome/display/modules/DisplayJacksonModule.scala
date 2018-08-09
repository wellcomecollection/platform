package uk.ac.wellcome.display.modules

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.twitter.finatra.json.modules.FinatraJacksonModule
import uk.ac.wellcome.display.serialize.WorksIncludesDeserializerModule

object DisplayJacksonModule extends FinatraJacksonModule {
  override val propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
  override val additionalJacksonModules = Seq(
    new WorksIncludesDeserializerModule
  )
}

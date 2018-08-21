package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.transformer.models.TransformerConfig

object TransformerModule extends TwitterModule {

  // This is a short-term hacky fix, so we don't validate the name of
  // the flag or give it a proper type.
  private val sourceName = flag[String](
    name = "transformer.sourceName",
    help = "Name of the transformer source (miro|sierra)"
  )

  @Provides
  @Singleton
  def providesTransformerConfig(): TransformerConfig =
    TransformerConfig(sourceName = sourceName())
}

package uk.ac.wellcome.sierra_adapter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import javax.inject.Singleton
import uk.ac.wellcome.models.transformable.SierraTransformable

import uk.ac.wellcome.utils.JsonUtil._

object SierraTransformableModule extends TwitterModule {
  @Provides
  @Singleton
  def provideIdentifiedWorkDecoder(): Decoder[SierraTransformable] =
    deriveDecoder[SierraTransformable]

  @Provides
  @Singleton
  def provideIdentifiedWorkEncoder(): Encoder[SierraTransformable] =
    deriveEncoder[SierraTransformable]
}

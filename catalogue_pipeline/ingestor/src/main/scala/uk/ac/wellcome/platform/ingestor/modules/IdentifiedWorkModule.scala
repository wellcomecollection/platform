package uk.ac.wellcome.platform.ingestor.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.utils.JsonUtil._

object IdentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideIdentifiedWorkDecoder(): Decoder[IdentifiedWork] =
    deriveDecoder[IdentifiedWork]

  @Provides
  @Singleton
  def provideIdentifiedWorkEncoder(): Encoder[IdentifiedWork] =
    deriveEncoder[IdentifiedWork]
}

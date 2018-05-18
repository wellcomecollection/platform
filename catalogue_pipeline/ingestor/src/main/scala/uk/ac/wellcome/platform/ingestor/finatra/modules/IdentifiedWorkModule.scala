package uk.ac.wellcome.platform.ingestor.finatra.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

import uk.ac.wellcome.utils.JsonUtil._

object IdentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[IdentifiedWork] =
    new IdentifiedWorkKeyPrefixGenerator()

  @Provides
  @Singleton
  def provideIdentifiedWorkDecoder(): Decoder[IdentifiedWork] =
    deriveDecoder[IdentifiedWork]

  @Provides
  @Singleton
  def provideIdentifiedWorkEncoder(): Encoder[IdentifiedWork] =
    deriveEncoder[IdentifiedWork]
}

class IdentifiedWorkKeyPrefixGenerator
    extends KeyPrefixGenerator[IdentifiedWork] {
  override def generate(obj: IdentifiedWork): String = {
    obj.sourceIdentifier.value.reverse.slice(0, 2)
  }
}

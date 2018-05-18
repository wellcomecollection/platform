package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

import uk.ac.wellcome.utils.JsonUtil._

object UnidentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[UnidentifiedWork] =
    new UnidentifiedWorkKeyPrefixGenerator()

  @Provides
  @Singleton
  def provideUnidentifiedWorkDecoder(): Decoder[UnidentifiedWork] =
    deriveDecoder[UnidentifiedWork]

  @Provides
  @Singleton
  def provideUnidentifiedWorkEncoder(): Encoder[UnidentifiedWork] =
    deriveEncoder[UnidentifiedWork]

}

class UnidentifiedWorkKeyPrefixGenerator
    extends KeyPrefixGenerator[UnidentifiedWork] {
  override def generate(id: String, obj: UnidentifiedWork): String = {
    obj.sourceIdentifier.value.reverse.slice(0, 2)
  }
}

package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.{Decoder, Encoder, Json}
import javax.inject.Singleton
import uk.ac.wellcome.platform.idminter.models.JsonKeyPrefixGenerator
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

object JsonModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[Json] =
    new JsonKeyPrefixGenerator()

  @Provides
  @Singleton
  def provideRecorderWorkEntryDecoder(): Decoder[Json] =
    Decoder.decodeJson

  @Provides
  @Singleton
  def provideRecorderWorkEntryEncoder(): Encoder[Json] =
    Encoder.encodeJson

}

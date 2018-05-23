package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.{Decoder, Encoder, Json}
import javax.inject.Singleton

object JsonModule extends TwitterModule {
  @Provides
  @Singleton
  def provideRecorderWorkEntryDecoder(): Decoder[Json] =
    Decoder.decodeJson

  @Provides
  @Singleton
  def provideRecorderWorkEntryEncoder(): Encoder[Json] =
    Encoder.encodeJson
}

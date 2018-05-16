package uk.ac.wellcome.platform.recorder.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.UnidentifiedWork

import uk.ac.wellcome.utils.JsonUtil._


object UnidentifiedWorkModule extends TwitterModule {
  @Provides
  @Singleton
  def provideUnidentifiedWorkDecoder(): Decoder[UnidentifiedWork] =
    deriveDecoder[UnidentifiedWork]

  @Provides
  @Singleton
  def provideUnidentifiedWorkEncoder(): Encoder[UnidentifiedWork] =
    deriveEncoder[UnidentifiedWork]

}
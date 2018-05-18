package uk.ac.wellcome.platform.matcher.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import javax.inject.Singleton
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry

import uk.ac.wellcome.utils.JsonUtil._

object RecorderWorkEntryModule extends TwitterModule {

  @Provides
  @Singleton
  def provideRecorderWorkEntryDecoder(): Decoder[RecorderWorkEntry] =
    deriveDecoder[RecorderWorkEntry]

  @Provides
  @Singleton
  def provideRecorderWorkEntryEncoder(): Encoder[RecorderWorkEntry] =
    deriveEncoder[RecorderWorkEntry]

}

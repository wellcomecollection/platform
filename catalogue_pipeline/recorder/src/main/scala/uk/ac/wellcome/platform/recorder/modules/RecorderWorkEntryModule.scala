package uk.ac.wellcome.platform.recorder.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.storage.s3.{KeyPrefixGenerator, SourcedKeyPrefixGenerator}
import uk.ac.wellcome.utils.JsonUtil._


object RecorderWorkEntryModule extends TwitterModule {

  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[RecorderWorkEntry] = {
    new SourcedKeyPrefixGenerator()
  }

  @Provides
  @Singleton
  def provideRecorderWorkEntryDecoder(): Decoder[RecorderWorkEntry] =
    deriveDecoder[RecorderWorkEntry]

  @Provides
  @Singleton
  def provideRecorderWorkEntryEncoder(): Encoder[RecorderWorkEntry] =
    deriveEncoder[RecorderWorkEntry]

}

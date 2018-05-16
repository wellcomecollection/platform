package uk.ac.wellcome.platform.recorder.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.storage.s3.{
  KeyPrefixGenerator,
  SourcedKeyPrefixGenerator
}

object RecorderWorkEntryKeyPrefixGeneratorModule extends TwitterModule {

  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[RecorderWorkEntry] = {
    new SourcedKeyPrefixGenerator()
  }
}

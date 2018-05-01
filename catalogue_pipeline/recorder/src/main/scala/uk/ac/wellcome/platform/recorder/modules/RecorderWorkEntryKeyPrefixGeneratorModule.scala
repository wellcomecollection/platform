package uk.ac.wellcome.platform.recorder.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.s3.{KeyPrefixGenerator, SourcedKeyPrefixGenerator}

object RecorderWorkEntryKeyPrefixGeneratorModule extends TwitterModule {

  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[RecorderWorkEntry] = {
    new SourcedKeyPrefixGenerator()
  }
}

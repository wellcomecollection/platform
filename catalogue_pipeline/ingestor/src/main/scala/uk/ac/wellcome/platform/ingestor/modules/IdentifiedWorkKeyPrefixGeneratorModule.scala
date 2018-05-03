package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

object IdentifiedWorkKeyPrefixGeneratorModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[IdentifiedWork] =
    new IdentifiedWorkKeyPrefixGenerator()
}

class IdentifiedWorkKeyPrefixGenerator
    extends KeyPrefixGenerator[IdentifiedWork] {
  override def generate(obj: IdentifiedWork): String = {
    obj.sourceIdentifier.value.reverse.slice(0, 2)
  }
}

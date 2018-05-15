package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import javax.inject.Singleton
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

object UnidentifiedWorkKeyPrefixGeneratorModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[UnidentifiedWork] =
    new UnidentifiedWorkKeyPrefixGenerator()
}

class UnidentifiedWorkKeyPrefixGenerator
    extends KeyPrefixGenerator[UnidentifiedWork] {
  override def generate(obj: UnidentifiedWork): String = {
    obj.sourceIdentifier.value.reverse.slice(0, 2)
  }
}

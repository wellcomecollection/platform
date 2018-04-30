package uk.ac.wellcome.transformer.modules

import javax.inject.Singleton
import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.s3.{KeyPrefixGenerator, SourcedKeyPrefixGenerator}


object UnidentifiedWorkKeyPrefixGeneratorModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[UnidentifiedWork] =
    new UnidentifiedWorkKeyPrefixGenerator()
}

class UnidentifiedWorkKeyPrefixGenerator extends KeyPrefixGenerator[UnidentifiedWork] {
  override def generate(obj: UnidentifiedWork): String = {
    obj.sourceIdentifier.value.reverse.slice(0, 2)
  }
}
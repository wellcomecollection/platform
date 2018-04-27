package uk.ac.wellcome.transformer.modules

import javax.inject.Singleton
import com.google.inject.{Inject, Provides}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.UnidentifiedWork
import uk.ac.wellcome.models.transformable.SierraTransformable
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
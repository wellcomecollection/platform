package uk.ac.wellcome.sierra_adapter.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.s3.{KeyPrefixGenerator, SourcedKeyPrefixGenerator}

object SierraKeyPrefixGeneratorModule extends TwitterModule {

  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[SierraTransformable] = {
    new SourcedKeyPrefixGenerator()
  }
}

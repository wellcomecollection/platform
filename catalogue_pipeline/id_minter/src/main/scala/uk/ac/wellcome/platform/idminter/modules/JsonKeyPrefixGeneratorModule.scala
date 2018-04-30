package uk.ac.wellcome.platform.idminter.modules

import javax.inject.Singleton
import com.google.inject.{Inject, Provides}
import com.twitter.inject.TwitterModule
import io.circe.Json
import uk.ac.wellcome.s3.KeyPrefixGenerator


object JsonKeyPrefixGeneratorModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[Json] =
    new UnidentifiedWorkKeyPrefixGenerator()
}

class UnidentifiedWorkKeyPrefixGenerator extends KeyPrefixGenerator[Json] {
  override def generate(obj: Json): String = {
    obj.hashCode().toString
  }
}
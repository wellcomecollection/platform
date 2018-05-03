package uk.ac.wellcome.platform.idminter.modules

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import io.circe.Json
import javax.inject.Singleton
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

object JsonKeyPrefixGeneratorModule extends TwitterModule {
  @Provides
  @Singleton
  def provideKeyPrefixGenerator(): KeyPrefixGenerator[Json] =
    new JsonKeyPrefixGenerator()
}

class JsonKeyPrefixGenerator extends KeyPrefixGenerator[Json] {
  override def generate(obj: Json): String = {
    obj.hashCode().toString
  }
}

package uk.ac.wellcome.platform.idminter.models

import io.circe.Json
import uk.ac.wellcome.storage.s3.KeyPrefixGenerator

class JsonKeyPrefixGenerator extends KeyPrefixGenerator[Json] {
  override def generate(obj: Json): String = {
    obj.hashCode().toString
  }
}

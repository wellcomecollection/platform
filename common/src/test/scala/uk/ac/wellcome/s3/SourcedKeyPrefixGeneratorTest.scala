package uk.ac.wellcome.s3

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Sourced

class SourcedKeyPrefixGeneratorTest extends FunSpec with Matchers {

 it("generates a prefixKey correctly") {
   val sourced = new Sourced {
     override val sourceId: String = "1234"
     override val sourceName: String = "sourceName"
   }

   val sourcedKeyPrefixGenerator = new SourcedKeyPrefixGenerator()
   val prefix = sourcedKeyPrefixGenerator.generate(sourced)

   prefix shouldBe "sourceName/43/1234"
 }
}

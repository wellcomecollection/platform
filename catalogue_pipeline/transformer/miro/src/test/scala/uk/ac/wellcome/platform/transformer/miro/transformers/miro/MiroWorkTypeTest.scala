package uk.ac.wellcome.platform.transformer.miro.transformers.miro

import org.scalatest.{FunSpec, Matchers}

class MiroWorkTypeTest extends FunSpec with Matchers {
  it("sets a WorkType of 'Digital images'") {
    transformer.getWorkType.isDefined shouldBe true
    transformer.getWorkType.get.label shouldBe "Digital images"
  }

  val transformer: transformers.MiroWorkType = new transformers.MiroWorkType {}
}

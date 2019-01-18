package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}

class MiroWorkTypeTest extends FunSpec with Matchers {
  it("sets a WorkType of 'Digital Images'") {
    transformer.getWorkType.isDefined shouldBe true
    transformer.getWorkType.get.label shouldBe "Digital Images"
  }

  val transformer = new MiroWorkType {}
}

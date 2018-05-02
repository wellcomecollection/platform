package uk.ac.wellcome.storage.type_classes

import org.scalatest.{FunSpec, Matchers}
import shapeless.LabelledGeneric

class IdGetterTest extends FunSpec with Matchers {

  case class TestId(something: String, id: String, somethingElse: String)

  it("creates a IdGetter instance for an hlist") {
    val gen = LabelledGeneric[TestId]

    val idGetter = IdGetter[gen.Repr]

    val id = "1111"
    idGetter.id(gen.to(TestId("something", id, "somethingElse"))) shouldBe id
  }

  it("creates a IdGetter instance for a case class") {
    val idGetter = IdGetter[TestId]

    val id = "1111"
    idGetter.id(TestId("something", id, "somethingElse")) shouldBe id
  }
}

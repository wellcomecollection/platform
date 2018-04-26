package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Concept, Subject}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraSubjectsTest extends FunSpec with Matchers {

  it("returns zero subjects if there are none") {
    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List()
    )
    assertExtractsSubjects(bibData, List())
  }

  it("returns subjects for 650 with only subfield a") {
    val expectedSubjects = List(
      Subject(
        label = "A Content",
        concepts = List(
          Concept(label = "A Content"))))

    assertExtractsSubjects(
      bibData("650",
        List(
          MarcSubfield(tag = "a", content = "A Content"))),
      expectedSubjects)
  }

  it("returns subjects for 650 with only subfields a and v") {
    val expectedSubjects = List(
      Subject(
        label = "A Content - V Content",
        concepts = List(
          Concept(label = "A Content"),
          Concept(label = "V Content"))))

    assertExtractsSubjects(bibData("650", List(
      MarcSubfield(tag = "a", content = "A Content"),
      MarcSubfield(tag = "v", content = "V Content")
    )),
      expectedSubjects)
  }

  it("subfield a is always first concept when returning subjects for 650 with subfields a, v") {
    val expectedSubjects = List(
      Subject(
        label = "A Content - V Content",
        concepts = List(
          Concept(label = "A Content"),
          Concept(label = "V Content"))))

    assertExtractsSubjects(bibData("650", List(
      MarcSubfield(tag = "v", content = "V Content"),
      MarcSubfield(tag = "a", content = "A Content")
    )),
      expectedSubjects)
  }

  it("returns subjects for 650 with subfields a, v, and x") {
    val expectedSubjects = List(
      Subject(
        label = "A Content - V Content - X Content",
        concepts = List(
          Concept(label = "A Content"),
          Concept(label = "V Content"),
          Concept(label = "X Content")
        )))

    assertExtractsSubjects(bibData("650", List(
      MarcSubfield(tag = "x", content = "X Content"),
      MarcSubfield(tag = "v", content = "V Content"),
      MarcSubfield(tag = "a", content = "A Content")
    )),
      expectedSubjects)
  }

  val transformer = new SierraSubjects {}

  private def assertExtractsSubjects(bibData: SierraBibData, expected: List[Subject]) = {
    transformer.getSubjects(bibData = bibData) shouldBe expected
  }

  private def bibData(marcTag: String, marcSubfields: List[MarcSubfield]) = {
    SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = marcTag,
          indicator1 = "",
          indicator2 = "",
          subfields = marcSubfields
          )
        )
    )
  }
}ª

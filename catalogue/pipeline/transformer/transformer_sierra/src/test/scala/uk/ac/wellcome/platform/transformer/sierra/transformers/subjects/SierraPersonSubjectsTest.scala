package uk.ac.wellcome.platform.transformer.sierra.transformers.subjects

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraPersonSubjectsTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {
  private val transformer = new SierraPersonSubjects {}

  it("returns zero subjects if there are none") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getSubjectsWithPerson(bibData) shouldBe Nil
  }

  it("returns subjects for tag 600 with only subfield a") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "A Content")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "A Content",
        concepts = List(Unidentifiable(Person(label = "A Content")))))

  }

  it("returns subjects for tag 600 with only subfields a and c") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "Larrey, D. J."),
            MarcSubfield(tag = "c", content = "baron")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "Larrey, D. J. baron",
        concepts = List(Unidentifiable(
          Person(label = "Larrey, D. J.", prefix = Some("baron"))))))
  }

  it("returns subjects for tag 600 with only subfields a and multiple c") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "David Attenborough"),
            MarcSubfield(tag = "c", content = "sir"),
            MarcSubfield(tag = "c", content = "doctor")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "David Attenborough sir doctor",
        concepts = List(Unidentifiable(
          Person(label = "David Attenborough", prefix = Some("sir doctor"))))))
  }

  it("returns subjects for tag 600 with only subfields a and b") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "David Attenborough"),
            MarcSubfield(tag = "b", content = "II")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "David Attenborough II",
        concepts = List(Unidentifiable(
          Person(label = "David Attenborough", numeration = Some("II"))))))
  }

  it("returns subjects for tag 600 with subfields a and e") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "David Attenborough,"),
            MarcSubfield(tag = "e", content = "author")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "David Attenborough, author",
        concepts = List(Unidentifiable(Person(label = "David Attenborough,")))))
  }

  it("returns subjects for tag 600 with subfields a and d") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "Rita Levi Montalcini,"),
            MarcSubfield(
              tag = "d",
              content = "22 April 1909 – 30 December 2012")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(Subject(
      label = "Rita Levi Montalcini, 22 April 1909 – 30 December 2012",
      concepts = List(Unidentifiable(Person(label = "Rita Levi Montalcini,")))))
  }

  it("returns subjects for tag 600 with subfields a and multiple e") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List(
            MarcSubfield(tag = "a", content = "David Attenborough,"),
            MarcSubfield(tag = "e", content = "author,"),
            MarcSubfield(tag = "e", content = "editor")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "David Attenborough, author, editor",
        concepts = List(Unidentifiable(Person(label = "David Attenborough,")))))
  }

  // There's nothing useful we can do here.  Arguably it's a cataloguing
  // error, but all the person will do is delete the field, so we can avoid
  // throwing an error.
  it("errors transforming a subject 600 if subfield a is missing") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          subfields = List()
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List()
  }

  it(
    "creates an identifiable subject with library of congress heading if there is a subfield 0 and the second indicator is 0") {
    val name = "Gerry the Garlic"
    val lcshCode = "lcsh7212"

    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          indicator2 = "0",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode)
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "Gerry the Garlic",
        concepts = List(
          Identifiable(
            Person(label = "Gerry the Garlic"),
            sourceIdentifier =
              SourceIdentifier(IdentifierType("lc-names"), "Person", lcshCode)))
      ))
  }

  it("creates an unidentifiable person concept if second indicator is not 0") {
    val name = "Gerry the Garlic"
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "600",
          indicator2 = "2",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = "mesh/456")
          )
        )
      )
    )

    transformer.getSubjectsWithPerson(sierraBibData) shouldBe List(
      Subject(
        label = "Gerry the Garlic",
        concepts = List(Unidentifiable(Person(label = "Gerry the Garlic")))))
  }
}

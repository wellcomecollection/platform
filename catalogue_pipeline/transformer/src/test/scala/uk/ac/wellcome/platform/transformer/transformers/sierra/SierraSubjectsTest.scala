package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData, VarField}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraSubjectsTest extends FunSpec with Matchers with SierraDataUtil {

  it("returns zero subjects if there are none") {
    val bibData = createSierraBibDataWith(varFields = List())
    assertExtractsSubjects(bibData, List())
  }
describe("Subjects from 650, 648 and 651 tags"){
  it("returns subjects for tag 650 with only subfield a") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content",
          concepts = List(Unidentifiable(Concept(label = "A Content")))))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedSubjects)
  }

  it("returns subjects for tag 650 with only subfields a and v") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content")))))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedSubjects)
  }

  it(
    "subfield a is always first concept when returning subjects for tag 650 with subfields a, v") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content")))))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "v", content = "V Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedSubjects)
  }

  it("returns subjects for tag 650 subfields a, v, and x") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        ))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "x", content = "X Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedSubjects
    )
  }

  it("returns subjects for tag 650 with subfields a, y") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - Y Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Period(label = "Y Content"))
          )))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "y", content = "Y Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedSubjects)
  }

  it("returns subjects for tag 650 with subfields a, z") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - Z Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Place(label = "Z Content"))
          )))

    assertExtractsSubjects(
      bibData(
        "650",
        List(
          MarcSubfield(tag = "z", content = "Z Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedSubjects)
  }

  it("returns subjects for multiple 650 tags with different subfields") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = "A1 Content"),
            MarcSubfield(tag = "z", content = "Z1 Content")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = "A2 Content"),
            MarcSubfield(tag = "v", content = "V2 Content")
          )
        )
      )
    )

    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A1 Content - Z1 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A1 Content")),
            Unidentifiable(Place(label = "Z1 Content"))
          )),
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A2 Content - V2 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A2 Content")),
            Unidentifiable(Concept(label = "V2 Content"))
          ))
      )

    assertExtractsSubjects(bibData, expectedSubjects)
  }

  it("returns subjects with primary concept Period for tag 648") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Period(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        ))

    assertExtractsSubjects(
      bibData(
        "648",
        List(
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "x", content = "X Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedSubjects
    )
  }

  it("returns subjects with primary concept Place for tag 651") {
    val expectedSubjects =
      List(
        Subject[MaybeDisplayable[AbstractConcept]](
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Place(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        ))

    assertExtractsSubjects(
      bibData(
        "651",
        List(
          MarcSubfield(tag = "x", content = "X Content"),
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedSubjects
    )
  }

  it(s"gets identifiers from subfield $$0") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          // LCSH heading
          indicator2 = "0",
          subfields = List(
            MarcSubfield(tag = "a", content = "absence"),
            MarcSubfield(tag = "0", content = "lcsh/123")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          // MESH heading
          indicator2 = "2",
          subfields = List(
            MarcSubfield(tag = "a", content = "abolition"),
            MarcSubfield(tag = "0", content = "mesh/456")
          )
        )
      )
    )

    val expectedSourceIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType("lc-subjects"),
        value = "lcsh/123",
        ontologyType = "Concept"
      ),
      SourceIdentifier(
        identifierType = IdentifierType("nlm-mesh"),
        value = "mesh/456",
        ontologyType = "Concept"
      )
    )

    val actualSourceIdentifiers = transformer
      .getSubjects(bibData)
      .map {
        _.concepts.head
      }
      .map {
        case Identifiable(_: Concept, sourceIdentifier, _, _) =>
          sourceIdentifier
        case other => assert(false, other)
      }

    expectedSourceIdentifiers shouldBe actualSourceIdentifiers
  }

  it("ignores subject with second indicator 7") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          indicator2 = "7",
          subfields = List(
            MarcSubfield(tag = "a", content = "absence"),
            MarcSubfield(tag = "0", content = "lcsh/123")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          // MESH heading
          indicator2 = "2",
          subfields = List(
            MarcSubfield(tag = "a", content = "abolition"),
            MarcSubfield(tag = "0", content = "mesh/456")
          )
        )
      )
    )

    transformer
      .getSubjects(bibData) shouldBe List(
      Subject(
        "abolition",
        List(
          Identifiable(
            Concept("abolition"),
            SourceIdentifier(
              identifierType = IdentifierType("nlm-mesh"),
              value = "mesh/456",
              ontologyType = "Concept"
            )))
      ))
  }

  it("Ignores a subject with second indicator 7 but no subfield 0") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "650",
          indicator1 = "",
          indicator2 = "7",
          subfields = List(
            MarcSubfield(tag = "a", content = "abolition")
          )
        )
      )
    )

    transformer
      .getSubjects(bibData) shouldBe List()
  }
}

  describe("tag 600 and 610 subjects") {
    it("returns subjects for tag 600 with only subfield a") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "A Content")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
          label = "A Content",
          concepts = List(Unidentifiable(Person(label = "A Content")))))

    }

    it("returns subjects for tag 600 with only subfields a and c") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "c", content = "Sir")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
          label = "Sir David Attenborough",
          concepts = List(
            Unidentifiable(Person(label = "David Attenborough", prefix = Some("Sir"))))))
    }

    it("returns subjects for tag 600 with only subfields a and multiple c") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "c", content = "Sir"),
          MarcSubfield(tag = "c", content = "Doctor")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
          label = "Sir Doctor David Attenborough",
          concepts = List(
            Unidentifiable(Person(label = "David Attenborough", prefix = Some("Sir Doctor"))))))
    }

    it("returns subjects for tag 600 with only subfields a and b") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "b", content = "II")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
        label = "David Attenborough II",
        concepts = List(
          Unidentifiable(Person(label = "David Attenborough", numeration = Some("II"))))))
    }

    it("returns subjects for tag 600 with subfields a and e") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "e", content = "author")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
        label = "David Attenborough, author",
        concepts = List(
          Unidentifiable(Person(label = "David Attenborough")))))
    }

    // TODO: re-enable once we're sure how dates should be rendered
    ignore("returns subjects for tag 600 with subfields a and d") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "d", content = "1800's")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
        label = "David Attenborough",
        concepts = List(
          Unidentifiable(Person(label = "David Attenborough")))))
    }

    it("returns subjects for tag 600 with subfields a and multiple e") {
      val sierraBibData = bibData(
        "600",
        List(
          MarcSubfield(tag = "a", content = "David Attenborough"),
          MarcSubfield(tag = "e", content = "author"),
          MarcSubfield(tag = "e", content = "editor")
        ))

      transformer.getSubjects(sierraBibData) shouldBe List(Subject(
        label = "David Attenborough, author, editor",
        concepts = List(
          Unidentifiable(Person(label = "David Attenborough")))))
    }

    // This test case shouldn't be possible but we've seen cataloguing errors
    // elsewhere. For now, we error in those cases so that we are able
    // to flag cataloguing errors, so error here as well for consistency
    it("errors transforming a subject 600 if subfield a is missing") {
      val sierraBibData = bibData(
        "600",
        List())

      intercept[TransformerException] {
        transformer.getSubjects(sierraBibData)
      }
    }
  }
  private val transformer = new SierraSubjects {}

  private def assertExtractsSubjects(
    bibData: SierraBibData,
    expected: List[Subject[MaybeDisplayable[AbstractConcept]]]) = {
    transformer.getSubjects(bibData = bibData) shouldBe expected
  }

  private def bibData(marcTag: String, marcSubfields: List[MarcSubfield]) = {
    createSierraBibDataWith(
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
}

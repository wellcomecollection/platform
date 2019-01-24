package uk.ac.wellcome.platform.transformer.sierra.transformers.subjects

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraConceptSubjectsTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {
  private val transformer = new SierraConceptSubjects {}

  it("returns zero subjects if there are none") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getSubjectswithAbstractConcepts(bibData) shouldBe Nil
  }

  it("returns subjects for tag 650 with only subfield a") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "a", content = "A Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content",
          concepts = List(Unidentifiable(Concept(label = "A Content")))
        )
      )
    )

  }

  it("returns subjects for tag 650 with only subfields a and v") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "a", content = "A Content"),
            MarcSubfield(tag = "v", content = "V Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content")))
        )
      )
    )
  }

  it(
    "subfield a is always first concept when returning subjects for tag 650 with subfields a, v") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "v", content = "V Content"),
            MarcSubfield(tag = "a", content = "A Content")
          )
        )
      )
    )
    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content")))
        )
      )
    )
  }

  it("returns subjects for tag 650 subfields a, v, and x") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "a", content = "A Content"),
            MarcSubfield(tag = "x", content = "X Content"),
            MarcSubfield(tag = "v", content = "V Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        )
      )
    )
  }

  it("returns subjects for tag 650 with subfields a, y") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "y", content = "Y Content"),
            MarcSubfield(tag = "a", content = "A Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - Y Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Period(label = "Y Content"))
          )
        )
      )
    )
  }

  it("returns subjects for tag 650 with subfields a, z") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "z", content = "Z Content"),
            MarcSubfield(tag = "a", content = "A Content")
          )
        )
      )
    )
    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - Z Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Place(label = "Z Content"))
          )
        )
      )
    )
  }

  it("returns subjects for multiple 650 tags with different subfields") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "a", content = "A1 Content"),
            MarcSubfield(tag = "z", content = "Z1 Content")
          )
        ),
        createVarFieldWith(
          marcTag = "650",
          subfields = List(
            MarcSubfield(tag = "a", content = "A2 Content"),
            MarcSubfield(tag = "v", content = "V2 Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(bibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A1 Content - Z1 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A1 Content")),
            Unidentifiable(Place(label = "Z1 Content"))
          )
        )
      ),
      Unidentifiable(
        Subject(
          label = "A2 Content - V2 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A2 Content")),
            Unidentifiable(Concept(label = "V2 Content"))
          )
        )
      )
    )
  }

  it("returns subjects with primary concept Period for tag 648") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "648",
          subfields = List(
            MarcSubfield(tag = "a", content = "A Content"),
            MarcSubfield(tag = "x", content = "X Content"),
            MarcSubfield(tag = "v", content = "V Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Period(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        )
      )
    )
  }

  it("returns subjects with primary concept Place for tag 651") {
    val sierraBibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "651",
          subfields = List(
            MarcSubfield(tag = "x", content = "X Content"),
            MarcSubfield(tag = "a", content = "A Content"),
            MarcSubfield(tag = "v", content = "V Content")
          )
        )
      )
    )

    transformer.getSubjectswithAbstractConcepts(sierraBibData) shouldBe List(
      Unidentifiable(
        Subject(
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Place(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        )
      )
    )
  }

  it(s"gets identifiers from subfield $$0") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          // LCSH heading
          indicator2 = "0",
          subfields = List(
            MarcSubfield(tag = "a", content = "absence"),
            MarcSubfield(tag = "0", content = "lcsh/123")
          )
        ),
        createVarFieldWith(
          marcTag = "650",
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
        ontologyType = "Subject"
      ),
      SourceIdentifier(
        identifierType = IdentifierType("nlm-mesh"),
        value = "mesh/456",
        ontologyType = "Subject"
      )
    )

    val actualSourceIdentifiers = transformer
      .getSubjectswithAbstractConcepts(bibData)
      .map {
        case Identifiable(
            _: Subject[MaybeDisplayable[AbstractConcept]],
            sourceIdentifier,
            _,
            _) =>
          sourceIdentifier
        case other => assert(false, other)
      }

    expectedSourceIdentifiers shouldBe actualSourceIdentifiers
  }

  it("ignores subject with second indicator 7") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          indicator2 = "7",
          subfields = List(
            MarcSubfield(tag = "a", content = "absence"),
            MarcSubfield(tag = "0", content = "lcsh/123")
          )
        ),
        createVarFieldWith(
          marcTag = "650",
          // MESH heading
          indicator2 = "2",
          subfields = List(
            MarcSubfield(tag = "a", content = "abolition"),
            MarcSubfield(tag = "0", content = "mesh/456")
          )
        )
      )
    )

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("nlm-mesh"),
      value = "mesh/456",
      ontologyType = "Subject"
    )

    transformer
      .getSubjectswithAbstractConcepts(bibData) shouldBe List(
      Identifiable(
        Subject(
          label = "abolition",
          concepts = List(
            Unidentifiable(Concept("abolition"))
          )
        ),
        sourceIdentifier = sourceIdentifier
      )
    )
  }

  it("Ignores a subject with second indicator 7 but no subfield 0") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "650",
          indicator2 = "7",
          subfields = List(
            MarcSubfield(tag = "a", content = "abolition")
          )
        )
      )
    )

    transformer
      .getSubjectswithAbstractConcepts(bibData) shouldBe List()
  }
}

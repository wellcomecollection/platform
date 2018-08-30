package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.utils.SierraDataGenerators

class SierraGenresTest extends FunSpec with Matchers with SierraDataGenerators {

  it("returns zero genres if there are none") {
    val bibData = createSierraBibDataWith(varFields = List())
    assertExtractsGenres(bibData, List())
  }

  it("returns genres for tag 655 with only subfield a") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content",
          concepts = List(Unidentifiable(Concept(label = "A Content")))))

    assertExtractsGenres(
      bibData("655", List(MarcSubfield(tag = "a", content = "A Content"))),
      expectedGenres)
  }

  it("returns subjects for tag 655 with subfields a and v") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        )
      )

    assertExtractsGenres(
      bibData(
        "655",
        List(
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedGenres)
  }

  it(
    "subfield a is always first concept when returning subjects for tag 655 with subfields a, v") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        )
      )

    assertExtractsGenres(
      bibData(
        "655",
        List(
          MarcSubfield(tag = "v", content = "V Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedGenres)
  }

  it("returns genres for tag 655 subfields a, v, and x") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content - X Content - V Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Concept(label = "X Content")),
            Unidentifiable(Concept(label = "V Content"))
          )
        ))

    assertExtractsGenres(
      bibData(
        "655",
        List(
          MarcSubfield(tag = "a", content = "A Content"),
          MarcSubfield(tag = "x", content = "X Content"),
          MarcSubfield(tag = "v", content = "V Content")
        )),
      expectedGenres
    )
  }

  it("returns subjects for tag 655 with subfields a, y") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content - Y Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Period(label = "Y Content"))
          )))

    assertExtractsGenres(
      bibData(
        "655",
        List(
          MarcSubfield(tag = "y", content = "Y Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedGenres)
  }

  it("returns subjects for tag 655 with subfields a, z") {
    val expectedGenres =
      List(
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A Content - Z Content",
          concepts = List(
            Unidentifiable(Concept(label = "A Content")),
            Unidentifiable(Place(label = "Z Content"))
          )))

    assertExtractsGenres(
      bibData(
        "655",
        List(
          MarcSubfield(tag = "z", content = "Z Content"),
          MarcSubfield(tag = "a", content = "A Content")
        )),
      expectedGenres)
  }

  it("returns subjects for multiple 655 tags with different subfields") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "655",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = "A1 Content"),
            MarcSubfield(tag = "z", content = "Z1 Content")
          )
        ),
        VarField(
          fieldTag = "p",
          marcTag = "655",
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
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A1 Content - Z1 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A1 Content")),
            Unidentifiable(Place(label = "Z1 Content"))
          )),
        Genre[MaybeDisplayable[AbstractConcept]](
          label = "A2 Content - V2 Content",
          concepts = List(
            Unidentifiable(Concept(label = "A2 Content")),
            Unidentifiable(Concept(label = "V2 Content"))
          ))
      )
    assertExtractsGenres(bibData, expectedSubjects)
  }

  it(s"gets identifiers from subfield $$0") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "655",
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
          marcTag = "655",
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
      .getGenres(bibData)
      .map { _.concepts.head }
      .map {
        case Identifiable(_: Concept, sourceIdentifier, _, _) =>
          sourceIdentifier
        case other => assert(false, other)
      }

    expectedSourceIdentifiers shouldBe actualSourceIdentifiers
  }

  private val transformer = new SierraGenres {}

  private def assertExtractsGenres(
    bibData: SierraBibData,
    expected: List[Genre[MaybeDisplayable[AbstractConcept]]]) = {
    transformer.getGenres(bibData) shouldBe expected
  }
}

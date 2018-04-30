package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Concept, Genre, Period, Place}
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

class SierraGenresTest extends FunSpec with Matchers {

  it("returns zero genres if there are none") {
    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List()
    )
    assertExtractsGenres(bibData, List())
  }

  it("returns genres for tag 655 with only subfield a") {
    val expectedGenres =
      List(
        Genre(
          label = "A Content",
          concepts = List(Concept(label = "A Content"))))

    assertExtractsGenres(
      bibData("655", List(MarcSubfield(tag = "a", content = "A Content"))),
      expectedGenres)
  }

  it("returns subjects for tag 655 with subfields a and v") {
    val expectedGenres =
      List(
        Genre(
          label = "A Content - V Content",
          concepts =
            List(Concept(label = "A Content"), Concept(label = "V Content"))))

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
        Genre(
          label = "A Content - V Content",
          concepts =
            List(Concept(label = "A Content"), Concept(label = "V Content"))))

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
        Genre(
          label = "A Content - X Content - V Content",
          concepts = List(
            Concept(label = "A Content"),
            Concept(label = "X Content"),
            Concept(label = "V Content")
          )))

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
        Genre(
          label = "A Content - Y Content",
          concepts = List(
            Concept(label = "A Content"),
            Period(label = "Y Content")
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
        Genre(
          label = "A Content - Z Content",
          concepts = List(
            Concept(label = "A Content"),
            Place(label = "Z Content")
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
    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
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
        Genre(
          label = "A1 Content - Z1 Content",
          concepts = List(
            Concept(label = "A1 Content"),
            Place(label = "Z1 Content")
          )),
        Genre(
          label = "A2 Content - V2 Content",
          concepts = List(
            Concept(label = "A2 Content"),
            Concept(label = "V2 Content")
          ))
      )
    assertExtractsGenres(bibData, expectedSubjects)
  }

  private val transformer = new SierraGenres {}

  private def assertExtractsGenres(bibData: SierraBibData,
                                   expected: List[Genre]) = {
    transformer.getGenres(bibData) shouldBe expected
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
}

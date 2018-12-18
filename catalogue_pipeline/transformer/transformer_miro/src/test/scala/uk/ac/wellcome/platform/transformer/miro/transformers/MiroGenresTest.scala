package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Concept, Genre, Unidentifiable}
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

class MiroGenresTest
    extends FunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("has an empty genre list on records without keywords") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecord,
      expectedGenreLabels = List()
    )
  }

  it("uses the image_phys_format field if present") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("Painting")
      ),
      expectedGenreLabels = List("Painting")
    )
  }

  it("uses the image_lc_genre field if present") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        lcGenre = Some("Sculpture")
      ),
      expectedGenreLabels = List("Sculpture")
    )
  }

  it("uses the image_phys_format and image_lc_genre fields if both present") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("Etching"),
        lcGenre = Some("Woodwork")
      ),
      expectedGenreLabels = List("Etching", "Woodwork")
    )
  }

  it("deduplicates entries in the genre field") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("Oil painting"),
        lcGenre = Some("Oil painting")
      ),
      expectedGenreLabels = List("Oil painting")
    )
  }

  it("normalises genre labels and concepts to sentence case") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("etching"),
        lcGenre = Some("wood work")
      ),
      expectedGenreLabels = List("Etching", "Wood work")
    )
  }

  private def transformRecordAndCheckGenres(
    miroRecord: MiroRecord,
    expectedGenreLabels: List[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val expectedGenres = expectedGenreLabels.map { label =>
      Genre(label, concepts = List(Unidentifiable(Concept(label))))
    }
    transformedWork.genres shouldBe expectedGenres
  }
}

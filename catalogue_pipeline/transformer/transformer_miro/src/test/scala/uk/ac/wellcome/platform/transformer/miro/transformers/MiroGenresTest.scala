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
        physFormat = Some("painting")
      ),
      expectedGenreLabels = List("painting")
    )
  }

  it("uses the image_lc_genre field if present") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        lcGenre = Some("sculpture")
      ),
      expectedGenreLabels = List("sculpture")
    )
  }

  it("uses the image_phys_format and image_lc_genre fields if both present") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("etching"),
        lcGenre = Some("woodwork")
      ),
      expectedGenreLabels = List("etching", "woodwork")
    )
  }

  it("deduplicates entries in the genre field") {
    transformRecordAndCheckGenres(
      miroRecord = createMiroRecordWith(
        physFormat = Some("oil painting"),
        lcGenre = Some("oil painting")
      ),
      expectedGenreLabels = List("oil painting")
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

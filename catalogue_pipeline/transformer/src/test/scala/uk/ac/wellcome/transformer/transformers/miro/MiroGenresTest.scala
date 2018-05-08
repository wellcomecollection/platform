package uk.ac.wellcome.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Concept,
  Genre,
  MaybeDisplayable,
  Unidentifiable
}
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper

class MiroGenresTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("has an empty genre list on records without keywords") {
    transformRecordAndCheckGenres(
      data = s""""image_title": "The giraffe's genre is gone'"""",
      expectedGenres = List()
    )
  }

  it("uses the image_phys_format field if present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A goat grazes on some grass",
        "image_phys_format": "painting"
      """,
      expectedGenres = List(Genre("painting", List(Unidentifiable(Concept("painting")))))
    )
  }

  it("uses the image_lc_genre field if present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "Grouchy geese are good as guards",
        "image_lc_genre": "sculpture"
      """,
      expectedGenres = List(Genre("sculpture", List(Unidentifiable(Concept("sculpture")))))
    )
  }

  it("uses the image_phys_format and image_lc_genre fields if both present") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A gorilla and a gibbon in a garden",
        "image_phys_format": "etching",
        "image_lc_genre": "woodwork"
      """,
      expectedGenres = List(
        Genre("etching", List(Unidentifiable(Concept("etching")))),
        Genre("woodwork", List(Unidentifiable(Concept("woodwork"))))
      )
    )
  }

  it("deduplicates entries in the genre field") {
    transformRecordAndCheckGenres(
      data = s"""
        "image_title": "A duality of dancing dodos",
        "image_phys_format": "oil painting",
        "image_lc_genre": "oil painting"
      """,
      expectedGenres =
        List(Genre("oil painting", List(Unidentifiable(Concept("oil painting")))))
    )
  }

  private def transformRecordAndCheckGenres(
    data: String,
    expectedGenres: List[Genre[MaybeDisplayable[AbstractConcept]]] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.genres shouldBe expectedGenres
  }
}

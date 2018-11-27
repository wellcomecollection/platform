package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Concept, Genre, Unidentifiable}

class MiroGenresTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("has an empty genre list on records without keywords") {
    transformRecordAndCheckGenres(
      title = "The giraffe's genre is gone'",
      expectedGenres = List()
    )
  }

  it("uses the image_phys_format field if present") {
    transformRecordAndCheckGenres(
      title = "A goat grazes on some grass",
      physFormat = "painting",
      expectedGenres = List("painting")
    )
  }

  it("uses the image_lc_genre field if present") {
    transformRecordAndCheckGenres(
      title = "Grouchy geese are good as guards",
      lcGenre = "sculpture",
      expectedGenres = List("sculpture")
    )
  }

  it("uses the image_phys_format and image_lc_genre fields if both present") {
    transformRecordAndCheckGenres(
      title = "A gorilla and a gibbon in a garden",
      physFormat = "etching",
      lcGenre = "woodwork",
      expectedGenres = List("etching", "woodwork")
    )
  }

  it("deduplicates entries in the genre field") {
    transformRecordAndCheckGenres(
      title = "A duality of dancing dodos",
      lcGenre = "oil painting",
      physFormat = "oil painting",
      expectedGenres = List("oil painting")
    )
  }

  private def transformRecordAndCheckGenres(
    title: String,
    physFormat: String = "",
    lcGenre: String = "",
    expectedGenres: List[String]
  ): Assertion = {
    val transformable = createMiroTransformableDataWith(
      title = Some(title),
      physFormat = if (physFormat.isEmpty) None else Some(physFormat),
      lcGenre = if (lcGenre.isEmpty) None else Some(lcGenre)
    )

    val expectedGenreObjects = expectedGenres
      .map { g: String =>
        Genre(g, List(Unidentifiable(Concept(g))))
      }

    transformWork(transformable).genres shouldBe expectedGenreObjects
  }
}

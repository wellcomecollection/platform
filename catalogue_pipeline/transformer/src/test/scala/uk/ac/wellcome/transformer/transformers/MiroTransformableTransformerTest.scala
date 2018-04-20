package uk.ac.wellcome.transformer.transformers

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.ac.wellcome.models._

class MiroTransformableTransformerTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should pass through the Miro identifier") {
    val MiroID = "M0000005_test"
    val work = transformWork(
      data = """"image_title": "A picture of a passing porpoise"""",
      MiroID = MiroID
    )
    work.identifiers shouldBe List(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", MiroID))
  }

  it("passes through the INNOPAC ID as the Sierra system number") {
    forAll(Table("", "b", "B", ".b", ".B")) { prefix =>
      forAll(Table("8", "x")) { checkDigit =>
        val innopacId = s"${prefix}1234567${checkDigit}"
        val expectedSierraNumber = s"b1234567${checkDigit}"

        transformRecordAndCheckSierraSystemNumber(
          innopacId = innopacId,
          expectedSierraNumber = s"b1234567${checkDigit}"
        )
      }
    }
  }

  describe("non-MPL references should be passed through as identifiers") {
    it("no references") {
      transformRecordAndCheckMiroLibraryReferences(
        data = """
          "image_library_ref_department": null,
          "image_library_ref_id": null
        """,
        expectedValues = List()
      )
    }

    it("one reference") {
      transformRecordAndCheckMiroLibraryReferences(
        data = """
          "image_library_ref_department": ["External Reference"],
          "image_library_ref_id": ["Sanskrit ID 1924"]
        """,
        expectedValues = List(
          "External Reference Sanskrit ID 1924"
        )
      )
    }

    it("two references") {
      transformRecordAndCheckMiroLibraryReferences(
        data =
          """
          "image_library_ref_department": ["External Reference", "ICV No"],
          "image_library_ref_id": ["Sanskrit ID 1924", "1234"]
        """,
        expectedValues = List(
          "External Reference Sanskrit ID 1924",
          "ICV No 1234"
        )
      )
    }

    it("with mismatched ref IDs/department") {
      assertTransformWorkFails(
        data = """
          "image_library_ref_department": ["External Reference"],
          "image_library_ref_id": ["Sanskrit ID 1924", "1234"]
        """
      )
    }

    it("with ref IDs null but department non-null") {
      assertTransformWorkFails(
        data = """
          "image_library_ref_department": ["External Reference"],
          "image_library_ref_id": null
        """
      )
    }

    it("with ref IDs non-null but department null") {
      assertTransformWorkFails(
        data = """
          "image_library_ref_department": null,
          "image_library_ref_id": ["Sanskrit ID 1924", "1234"]
        """
      )
    }
  }

  it("should have no description if no image_image_desc field is present") {
    val work = transformWork(data = s""""image_title": "A line of lions"""")
    work.description shouldBe None
  }

  it("should pass through the value of the description field") {
    val description = "A new novel about northern narwhals in November"
    val work = transformWork(
      data = s"""
        "image_title": "A note on narwhals",
        "image_image_desc": "$description"
      """
    )
    work.description shouldBe Some(description)
  }

  describe("Wellcome Images Awards metadata") {
    it("should do nothing for non-WIA metadata") {
      val description = "Spotting sea snakes on sandbanks."
      val work = transformWork(
        data = s"""
          "image_title": "Snakes!",
          "image_image_desc": "$description",
          "image_award": ["Award of Excellence"],
          "image_award_date": [null]
        """
      )
      work.description shouldBe Some(description)
    }

    it("should add WIA metadata if present") {
      val description = "Purple penguins play with paint."
      val work = transformWork(
        data = s"""
          "image_title": "Penguin feeding time",
          "image_image_desc": "$description",
          "image_award": ["Biomedical Image Awards"],
          "image_award_date": ["2001"]
        """
      )
      work.description shouldBe Some(
        description + " Biomedical Image Awards 2001.")
    }

    it("should only include WIA metadata") {
      val description = "Giraffes can be grazing, galloping or graceful."
      val work = transformWork(
        data = s"""
          "image_title": "A giraffe trifecta",
          "image_image_desc": "$description",
          "image_award": ["Dirt, Wellcome Collection", "Biomedical Image Awards"],
          "image_award_date": [null, "2002"]
        """
      )
      work.description shouldBe Some(
        description + " Biomedical Image Awards 2002.")
    }

    it("should combine multiple WIA metadata fields if necessary") {
      val description = "Amazed and awe-inspired by an adversarial aardvark."
      val work = transformWork(
        data = s"""
          "image_title": "Award-winning!",
          "image_image_desc": "$description",
          "image_award": ["WIA Overall Winner", "Wellcome Image Awards"],
          "image_award_date": ["2015", "2015"]
        """
      )
      work.description shouldBe Some(
        description + " Wellcome Image Awards Overall Winner 2015.")
    }
  }

  it("should pass through the value of the creation date on V records") {
    val date = "1820-1848"
    val work = transformWork(
      data = s"""
        "image_title": "A description of a dalmation",
        "image_image_desc": "A description of a dalmation with dots",
        "image_artwork_date": "$date"
      """,
      MiroCollection = "Images-V"
    )
    work.createdDate shouldBe Some(Period(date))
  }

  it("should not pass through the value of the creation date on non-V records") {
    val date = "1820-1848"
    val work = transformWork(
      data = s"""
        "image_title": "A diary about a dodo",
        "image_artwork_date": "$date"
      """,
      MiroCollection = "Images-A"
    )
    work.createdDate shouldBe None
  }

  it("should pass through the lettering field if available") {
    val lettering = "A lifelong lament for lemurs"
    val work = transformWork(
      data = s"""
        "image_title": "Lemurs and lemons",
        "image_supp_lettering": "$lettering"
      """
    )
    work.lettering shouldBe Some(lettering)
  }

  it("should correct HTML-encoded entities in the input JSON") {
    val work = transformWork(
      data = s"""
        "image_title": "A caf&#233; for cats",
        "image_creator": ["Gyokush&#333;, a c&#228;t &#212;wn&#234;r"]
      """
    )

    work.title shouldBe Some("A café for cats")

    // TODO: Replace this test with a check on "creators"
    // work.creators shouldBe List(Unidentifiable(Agent("Gyokushō, a cät Ôwnêr")))
  }

  private def transformRecordAndCheckSierraSystemNumber(
    innopacId: String,
    expectedSierraNumber: String,
    miroID: String = "V0000832"
  ) = {
    val work = transformWork(
      data = s"""
        "image_title": "A bouncing bundle of bison",
        "image_innopac_id": "$innopacId"
      """,
      MiroID = miroID
    )
    work.identifiers shouldBe List(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", miroID),
      SourceIdentifier(
        IdentifierSchemes.sierraSystemNumber,
        "Work",
        expectedSierraNumber)
    )
  }

  private def transformRecordAndCheckMiroLibraryReferences(
    data: String,
    expectedValues: List[String]
  ) = {
    val work = transformWork(
      data = s"""
        "image_title": "A fanciful frolicking of fish",
        $data
      """,
      MiroID = "V0175278"
    )
    val miroIDList = List(
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", "V0175278")
    )
    val libraryRefList = expectedValues.map {
      SourceIdentifier(IdentifierSchemes.miroLibraryReference, "Work", _)
    }
    work.identifiers shouldBe (miroIDList ++ libraryRefList)
  }
}

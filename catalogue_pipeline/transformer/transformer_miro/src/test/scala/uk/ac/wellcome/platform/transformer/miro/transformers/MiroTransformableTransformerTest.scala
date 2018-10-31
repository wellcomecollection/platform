package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._

class MiroTransformableTransformerTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("passes through the Miro identifier") {
    val MiroID = "M0000005_test"
    val work = transformWork(
      data = """"image_title": "A picture of a passing porpoise"""",
      MiroID = MiroID
    )
    work.identifiers shouldBe List(
      SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        "Work",
        MiroID))
  }

  it("passes through the INNOPAC ID as the Sierra system number") {
    forAll(Table("", "b", "B", ".b", ".B")) { prefix =>
      forAll(Table("8", "x")) { checkDigit =>
        val innopacId = s"${prefix}1234567${checkDigit}"
        val expectedSierraNumber = s"b1234567${checkDigit}"

        transformRecordAndCheckSierraSystemNumber(
          innopacId = innopacId,
          expectedSierraNumber = expectedSierraNumber
        )
      }
    }
  }

  describe("non-MPL references are passed through as identifiers") {
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
        data = """
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

  it("has no description if no image_image_desc field is present") {
    val work = transformWork(data = s""""image_title": "A line of lions"""")
    work.description shouldBe None
  }

  it("passes through the value of the description field") {
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
    it("does nothing for non-WIA metadata") {
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

    it("adds WIA metadata if present") {
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

    it("only includes WIA metadata") {
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

    it("combines multiple WIA metadata fields if necessary") {
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

  it("passes through the value of the creation date on V records") {
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

  it("does not pass through the value of the creation date on non-V records") {
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

  it("passes through the lettering field if available") {
    val lettering = "A lifelong lament for lemurs"
    val work = transformWork(
      data = s"""
        "image_title": "Lemurs and lemons",
        "image_supp_lettering": "$lettering"
      """
    )
    work.lettering shouldBe Some(lettering)
  }

  it("corrects HTML-encoded entities in the input JSON") {
    val work = transformWork(
      data = s"""
        "image_title": "A caf&#233; for cats",
        "image_creator": ["Gyokush&#333;, a c&#228;t &#212;wn&#234;r"]
      """
    )

    work.title shouldBe "A café for cats"
    work.contributors shouldBe List(
      Contributor(agent = Unidentifiable(Agent("Gyokushō, a cät Ôwnêr")))
    )
  }

  it(
    "returns an InvisibleWork if usage restrictions mean we suppress the image") {
    assertTransformReturnsInvisibleWork(
      data =
        buildJSONForWork("""
        "image_title": "Private pictures of perilous penguins",
        "image_use_restrictions": "Do not use"
      """)
    )
  }

  it("returns an InvisibleWork for images from contributor GUS") {
    assertTransformReturnsInvisibleWork(
      miroId = "B0009891",
      data = buildJSONForWork("""
        "image_source_code": "GUS"
      """)
    )
  }

  it("returns an InvisibleWork for images without copyright clearance") {
    assertTransformReturnsInvisibleWork(
      data = """{
        "image_cleared": "Y",
        "image_copyright_cleared": "N",
        "image_tech_file_size": ["1000000"],
        "image_use_restrictions": "CC-BY"
      }"""
    )
  }

  it(
    "transforms an image with no credit line and an image-specific contributor code") {
    val work = transformWork(
      data = s"""
        "image_credit_line": null,
        "image_source_code": "FDN"
      """,
      MiroID = "B0011308"
    )

    val expectedDigitalLocation = DigitalLocation(
      url = "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
      license = Some(License_CCBY),
      credit = Some("Ezra Feilden"),
      locationType = LocationType("iiif-image")
    )
    work.itemsV1.head.agent.locations shouldBe List(expectedDigitalLocation)
  }

  it("extracts both identifiable and unidentifiable items") {
    val work = transformWork(
      MiroID = "B0011308"
    )

    val expectedLocation = DigitalLocation(
      "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
      LocationType("iiif-image"),
      Some(License_CCBY),
      None)
    work.itemsV1 shouldBe List(
      Identifiable(
        Item(List(expectedLocation)),
        SourceIdentifier(
          IdentifierType("miro-image-number"),
          "Item",
          "B0011308")))
    work.items shouldBe List(Unidentifiable(Item(List(expectedLocation))))
  }

  it("sets the WorkType as 'Digital images'") {
    val work = transformWork()
    work.workType.isDefined shouldBe true
    work.workType.get.label shouldBe "Digital images"
  }

  private def assertTransformReturnsInvisibleWork(miroId: String = "G0000001",
                                                  data: String) = {
    val miroTransformable = createMiroTransformableWith(
      miroId = miroId,
      data = data
    )

    val triedMaybeWork = transformer.transform(miroTransformable, version = 1)
    triedMaybeWork.isSuccess shouldBe true

    triedMaybeWork.get shouldBe UnidentifiedInvisibleWork(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        ontologyType = "Work",
        value = miroTransformable.sourceId
      ),
      version = 1
    )
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
      SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        "Work",
        miroID),
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
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
      SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        "Work",
        "V0175278")
    )
    val libraryRefList = expectedValues.map {
      SourceIdentifier(
        identifierType = IdentifierType("miro-library-reference"),
        "Work",
        _)
    }
    work.identifiers shouldBe (miroIDList ++ libraryRefList)
  }
}

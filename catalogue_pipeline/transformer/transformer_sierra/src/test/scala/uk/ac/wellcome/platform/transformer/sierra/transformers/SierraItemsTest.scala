package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibNumber,
  SierraItemNumber
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraItemData
}
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLocation
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators

class SierraItemsTest extends FunSpec with Matchers with SierraDataGenerators {

  val transformer = new Object with SierraItems

  it("creates both forms of the Sierra ID in 'identifiers'") {
    val itemData = createSierraItemData
    val itemId = createSierraItemNumber

    val sourceIdentifier1 = createSierraSystemSourceIdentifierWith(
      ontologyType = "Item",
      value = itemId.withCheckDigit)

    val sourceIdentifier2 = SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      ontologyType = "Item",
      value = itemId.withoutCheckDigit
    )

    val expectedIdentifiers = List(sourceIdentifier1, sourceIdentifier2)

    val transformedItem: MaybeDisplayable[Item] = getTransformedItems(
      itemDataMap = Map(
        itemId -> itemData
      )
    ).head

    transformedItem
      .asInstanceOf[Identifiable[Item]]
      .identifiers shouldBe expectedIdentifiers
  }

  it("uses the full Sierra system number as the source identifier") {
    val itemId = createSierraItemNumber
    val sourceIdentifier = createSierraSystemSourceIdentifierWith(
      ontologyType = "Item",
      value = itemId.withCheckDigit
    )
    val itemData = createSierraItemData

    val transformedItem: MaybeDisplayable[Item] = getTransformedItems(
      itemDataMap = Map(itemId -> itemData)
    ).head

    transformedItem
      .asInstanceOf[Identifiable[Item]]
      .sourceIdentifier shouldBe sourceIdentifier
  }

  it("removes items with deleted=true") {
    val item1 = createSierraItemDataWith(deleted = true)
    val item2 = createSierraItemDataWith(deleted = false)

    val itemDataMap = Map(
      createSierraItemNumber -> item1,
      createSierraItemNumber -> item2
    )

    getTransformedItems(itemDataMap = itemDataMap) should have size 1
  }

  // Note: the following two tests are for historical reasons -- an old
  // version of this code used "v"/"e-book" as the distinction for whether
  // we would add a digital item.
  //
  // The current rule (2018-08-09) is to use the presence of the 'dlnk'
  // location, but I left a modified version of these tests in place to
  // check the old code wasn't left lying around!

  it(
    "does not create any items for an e-book record without the 'dlnk' location") {
    val bibData = createSierraBibDataWith(
      materialType = Some(createSierraMaterialTypeWith(code = "v"))
    )

    getTransformedItems(bibData = bibData) shouldBe List()
  }

  it(
    "does not create any items for a non e-book record without the 'dlnk' location") {
    val bibData = createSierraBibDataWith(
      materialType = Some(createSierraMaterialTypeWith(code = "x"))
    )

    getTransformedItems(bibData = bibData) shouldBe List()
  }

  it("ignores a digital item on a bib record without a 'dlnk' location") {
    val bibData = createSierraBibDataWith(
      locations = Some(
        List(
          SierraSourceLocation("digi", "Digitised Collections")
        ))
    )

    getTransformedItems(bibData = bibData) shouldBe List()
  }

  it("adds a digital item on a bib record with a 'dlnk' location") {
    val bibId = createSierraBibNumber
    val bibData = createSierraBibDataWith(
      locations = Some(
        List(
          SierraSourceLocation("digi", "Digitised Collections"),
          SierraSourceLocation("dlnk", "Digitised content")
        ))
    )

    val expectedItems = List(
      Unidentifiable(
        agent = Item(locations = List(DigitalLocation(
          url =
            s"https://wellcomelibrary.org/iiif/${bibId.withCheckDigit}/manifest",
          license = None,
          locationType = LocationType("iiif-presentation"))))
      )
    )

    getTransformedItems(bibId = bibId, bibData = bibData) shouldBe expectedItems
  }

  it("creates an item with a physical location") {
    val sierraLocation = SierraSourceLocation(
      code = "sghi2",
      name = "Closed stores Hist. 2"
    )
    val itemData = createSierraItemDataWith(location = Some(sierraLocation))

    val itemDataMap = Map(createSierraItemNumber -> itemData)

    val item = getTransformedItems(itemDataMap = itemDataMap).head
    item.agent.locations shouldBe List(
      PhysicalLocation(
        locationType = LocationType(sierraLocation.code),
        label = sierraLocation.name
      )
    )
  }

  it(
    "combines a single physical Item and a single digital Item into a single Item") {
    val sierraLocation = SierraSourceLocation(
      code = "scmac",
      name = "Closed stores Arch. & MSS"
    )

    val bibId = createSierraBibNumber
    val bibData = createSierraBibDataWith(
      locations = Some(
        List(
          SierraSourceLocation("dlnk", "Digitised content")
        ))
    )

    val itemDataMap = Map(
      createSierraItemNumber -> createSierraItemDataWith(
        location = Some(sierraLocation))
    )

    val result = getTransformedItems(
      bibId = bibId,
      bibData = bibData,
      itemDataMap = itemDataMap)

    result.size shouldBe 1
    result.head.agent.locations shouldBe List(
      PhysicalLocation(
        locationType = LocationType(sierraLocation.code),
        label = sierraLocation.name
      ),
      DigitalLocation(
        url =
          s"https://wellcomelibrary.org/iiif/${bibId.withCheckDigit}/manifest",
        license = None,
        locationType = LocationType("iiif-presentation")
      )
    )
  }

  it("doesn't combine Items if there's more than one digital item") {
    val sierraLocation1 = SierraSourceLocation(
      code = "sicon",
      name = "Closed stores Iconographic"
    )

    val sierraLocation2 = SierraSourceLocation(
      code = "sepbi",
      name = "Closed stores EPB Biog p.Vol"
    )

    val bibId = createSierraBibNumber
    val bibData = createSierraBibDataWith(
      locations = Some(
        List(
          SierraSourceLocation("dlnk", "Digitised content")
        ))
    )

    val itemDataMap = Map(
      createSierraItemNumber -> createSierraItemDataWith(
        location = Some(sierraLocation1)),
      createSierraItemNumber -> createSierraItemDataWith(
        location = Some(sierraLocation2))
    )

    val result = getTransformedItems(
      bibId = bibId,
      bibData = bibData,
      itemDataMap = itemDataMap)

    result.size shouldBe 3
  }

  private def getTransformedItems(
    bibId: SierraBibNumber = createSierraBibNumber,
    bibData: SierraBibData = createSierraBibData,
    itemDataMap: Map[SierraItemNumber, SierraItemData] = Map())
    : List[MaybeDisplayable[Item]] =
    transformer.getItems(
      bibId = bibId,
      bibData = bibData,
      itemDataMap = itemDataMap
    )
}

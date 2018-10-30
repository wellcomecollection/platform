package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators

class SierraTransformableTest
    extends FunSpec
    with Matchers
    with SierraGenerators {

  it("allows creation of SierraTransformable with no data") {
    SierraTransformable(sierraId = createSierraBibNumber)
  }

  it("allows creation from only a SierraBibRecord") {
    val bibRecord = createSierraBibRecord
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sierraId shouldEqual bibRecord.id
    mergedRecord.maybeBibRecord.get shouldEqual bibRecord
  }

  it("allows looking up items by ID") {
    val itemRecords = (0 to 3).map { _ =>
      createSierraItemRecord
    }.toList
    val transformable = createSierraTransformableWith(
      itemRecords = itemRecords
    )

    transformable.itemRecords(itemRecords.head.id) shouldBe itemRecords.head

    // The first one should work by identity (the ID is the same object
    // as the key).  Check it also works with a record number which is equal
    // but not identical.
    val recordNumber = SierraItemNumber(itemRecords.head.id.withoutCheckDigit)
    transformable.itemRecords(recordNumber) shouldBe itemRecords.head
  }
}

package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecordNumber
}
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Random

trait SierraUtil extends IdentifiersUtil {

  // A lot of Sierra tests (e.g. mergers) check the behaviour when merging
  // a record with a newer version, or vice versa.  Provide two dates here
  // for convenience.
  val olderDate: Instant = Instant.parse("1999-09-09T09:09:09Z")
  val newerDate: Instant = Instant.parse("2001-01-01T01:01:01Z")

  /** Returns a random digit as a string.  This is copied from the
    * definition of Random.alphanumeric.
    */
  private def randomNumeric: Stream[Char] = {
    def nextDigit: Char = {
      val chars = '0' to '9'
      chars charAt Random.nextInt(chars.length)
    }

    Stream continually nextDigit
  }

  def createSierraRecordNumberString: String =
    randomNumeric take 7 mkString

  def createSierraRecordNumber: SierraRecordNumber =
    SierraRecordNumber(createSierraRecordNumberString)

  def createSierraRecordNumberStrings(count: Int): List[String] =
    (1 to count).map { _ =>
      createSierraRecordNumberString
    }.toList

  def createSierraBibRecordWith(
    id: SierraRecordNumber = createSierraRecordNumber,
    data: String = "",
    modifiedDate: Instant = Instant.now): SierraBibRecord = {
    val recordData = if (data == "") {
      s"""{"id": "$id", "title": "${randomAlphanumeric(50)}"}"""
    } else data

    SierraBibRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate
    )
  }

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()

  def createSierraItemRecordWith(
    id: SierraRecordNumber = createSierraRecordNumber,
    data: String = "",
    modifiedDate: Instant = Instant.now,
    bibIds: List[String] = List(),
    unlinkedBibIds: List[String] = List(),
    version: Int = 0
  ): SierraItemRecord = {
    val recordData = if (data == "") {
      s"""
         |{
         |  "id": "$id",
         |  "updatedDate": "${modifiedDate.toString}",
         |  "bibIds": ${toJson(bibIds).get}
         |}
         |""".stripMargin
    } else data

    SierraItemRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds,
      version = version
    )
  }

  def createSierraItemRecord: SierraItemRecord = createSierraItemRecordWith()

  def createSierraTransformableWith(
    sierraId: SierraRecordNumber = createSierraRecordNumber,
    maybeBibRecord: Option[SierraBibRecord] = None,
    itemRecords: List[SierraItemRecord] = List()
  ): SierraTransformable =
    SierraTransformable(
      sierraId = sierraId,
      maybeBibRecord = maybeBibRecord,
      itemRecords = itemRecords.map { record: SierraItemRecord =>
        record.id -> record
      }.toMap
    )

  def createSierraTransformable: SierraTransformable =
    createSierraTransformableWith()
}

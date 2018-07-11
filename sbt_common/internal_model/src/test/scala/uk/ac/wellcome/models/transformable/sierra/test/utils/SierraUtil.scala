package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}

import scala.util.Random

trait SierraUtil {

  // A lot of Sierra tests (e.g. mergers) check the behaviour when merging
  // a record with a newer version, or vice versa.  Provide two dates here
  // for convenience.
  val olderDate: Instant = Instant.parse("1999-09-09T09:09:09Z")
  val newerDate: Instant = Instant.parse("2001-01-01T01:01:01Z")

  private def createSierraRecordStringWith(id: String): String =
    s"""
       |{
       |  "id": "$id"
       |}
    """.stripMargin

  def createSierraBibRecordWith(
    id: String = Random.alphanumeric take 7 mkString,
    data: String = "",
    modifiedDate: Instant = Instant.now
  ): SierraBibRecord =
    SierraBibRecord(
      id = id,
      data = if (data == "") createSierraRecordStringWith(id = id) else data,
      modifiedDate = modifiedDate
    )

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()

  def createSierraItemRecordWith(
    id: String = Random.alphanumeric take 7 mkString,
    data: String = "",
    modifiedDate: Instant = Instant.now,
    bibIds: List[String] = List()
  ): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = if (data == "") createSierraRecordStringWith(id = id) else data,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = List()
    )
}

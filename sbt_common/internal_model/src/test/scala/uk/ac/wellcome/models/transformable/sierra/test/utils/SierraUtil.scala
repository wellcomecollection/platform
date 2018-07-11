package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}

import scala.util.Random

trait SierraUtil {

  private def createSierraRecordStringWith(id: String): String =
    s"""
       |{
       |  "id": "$id"
       |}
    """.stripMargin

  def createSierraBibRecordWith(
    id: String = Random.alphanumeric take 7 mkString,
    data: String = "",
    modifiedDate: String
  ): SierraBibRecord =
    createSierraBibRecordWith(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )

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
    modifiedDate: String,
    bibIds: List[String] = List()
  ): SierraItemRecord =
    createSierraItemRecordWith(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate),
      bibIds = bibIds
    )

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

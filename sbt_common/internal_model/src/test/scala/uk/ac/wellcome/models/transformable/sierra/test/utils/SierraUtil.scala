package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

import scala.util.Random

trait SierraUtil {

  private def createSierraBibRecordStringWith(id: String): String =
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
      data = if (data == "") createSierraBibRecordStringWith(id = id) else data,
      modifiedDate = modifiedDate
    )

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()
}

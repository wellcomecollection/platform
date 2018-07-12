package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}

import scala.collection.immutable.Stream
import scala.util.Random

trait SierraUtil {

  // A lot of Sierra tests (e.g. mergers) check the behaviour when merging
  // a record with a newer version, or vice versa.  Provide two dates here
  // for convenience.
  val olderDate: Instant = Instant.parse("1999-09-09T09:09:09Z")
  val newerDate: Instant = Instant.parse("2001-01-01T01:01:01Z")

  /** Returns a random digit as a string.  This is copied from the
    * definition of Random.alphanumeric.
    *
    * @return
    */
  private def randomNumeric: Stream[Char] = {
    def nextAlphaNum: Char = {
      val chars = "0123456789"
      chars charAt Random.nextInt(chars.length)
    }

    Stream continually nextAlphaNum
  }

  /** Creates a Sierra ID, which must be a 7 digit number.  (Some of our
    * code explicitly expects this format, and errors if it gets a
    * non-numeric ID.)
    */
  private def createSierraId: String =
    randomNumeric take 7 mkString

  private def createSierraRecordStringWith(id: String): String =
    s"""
       |{
       |  "id": "$id"
       |}
    """.stripMargin

  def createSierraBibRecordWith(
    id: String = createSierraId,
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
    id: String = createSierraId,
    data: String = "",
    modifiedDate: Instant = Instant.now,
    bibIds: List[String] = List(createSierraId),
    unlinkedBibIds: List[String] = List(),
    version: Int = 0
  ): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = if (data == "") createSierraRecordStringWith(id = id) else data,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds,
      version = version
    )

  def createSierraItemRecord: SierraItemRecord = createSierraItemRecordWith()
}

package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraRecordNumber}

import scala.util.Random

trait SierraUtil {

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

  def createSierraBibRecordWith(
    id: String = createSierraRecordNumberString,
    data: String = "<<BibRecord>>"): SierraBibRecord =
    SierraBibRecord(
      id = id,
      data = data,
      modifiedDate = Instant.now
    )

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()
}

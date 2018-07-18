package uk.ac.wellcome.models.transformable.sierra.test.utils

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

import scala.util.Random

class SierraUtil {

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

  def createSierraRecordNumber: SierraRecordNumber =
    SierraRecordNumber(randomNumeric take 7 mkString)

}

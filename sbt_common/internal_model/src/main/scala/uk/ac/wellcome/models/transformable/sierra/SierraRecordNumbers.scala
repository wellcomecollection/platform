package uk.ac.wellcome.models.transformable.sierra

object SierraRecordTypes extends Enumeration {
  val bibs, items = Value
}

/** Utilities for working with Sierra "record numbers".
  *
  * These record numbers are the seven- or eight-digit IDs that
  * are used to identify resources in Sierra.
  */
object SierraRecordNumbers {

  /** Add the check digit to a Sierra ID.
    *
    * Examples:
    *
    * scala> addCheckDigit("1024364", recordType = SierraRecordTypes.bibs)
    * res0: String = b10243641
    *
    * scala> addCheckDigit("1952770", recordType = SierraRecordTypes.items)
    * res1: String = i19527706
    */
  def addCheckDigit(sierraId: String,
                    recordType: SierraRecordTypes.Value): String = {

    // First check that we're being passed a 7-digit numeric ID -- anything
    // else is an error.
    assert(
      is7digitRecordNumber(sierraId),
      s"addCheckDigit() can only be used with 7-digit record numbers; not $sierraId")

    val checkDigit = getCheckDigit(sierraId)

    // Then pick the record type prefix.
    val prefix = recordType match {
      case SierraRecordTypes.bibs  => "b"
      case SierraRecordTypes.items => "i"
      case _ =>
        throw new RuntimeException(
          s"Received unrecognised record type: $recordType"
        )
    }

    val recordNumber = s"$sierraId$checkDigit"
    assertIs8DigitRecordNumber(recordNumber)
    s"$prefix$recordNumber"
  }

  /** Returns the check digit that should be added to a record ID.
    *
    * Quoting from the Sierra manual:
    *
    *     Check digits may be any one of 11 possible digits (0, 1, 2, 3, 4,
    *     5, 6, 7, 8, 9, or x).
    *
    *     The check digit is calculated as follows:
    *
    *     Multiply the rightmost digit of the record number by 2, the next
    *     digit to the left by 3, the next by 4, etc., and total the products.
    *
    *     Divide the total by 11 and retain the remainder.  The remainder
    *     after the division is the check digit.  If the remainder is 10,
    *     the letter x is used as the check digit.
    *
    * This method can only be passed a 7-digit Sierra ID.
    *
    */
  private def getCheckDigit(sierraId: String): String = {
    assertIs7DigitRecordNumber(sierraId)
    val remainder = sierraId.reverse
      .zip(Stream from 2)
      .map { case (char: Char, count: Int) => char.toString.toInt * count }
      .sum % 11
    if (remainder == 10) "x" else remainder.toString
  }

  private def is7digitRecordNumber(s: String): Boolean =
    """^[0-9]{7}$""".r.unapplySeq(s) isDefined

  def assertIs7DigitRecordNumber(s: String): Unit =
    assert(is7digitRecordNumber(s), s"Not a 7-digit Sierra record number: $s")

  private def is8digitRecordNumber(s: String): Boolean =
    """^[0-9]{7}[0-9xX]$""".r.unapplySeq(s) isDefined

  def assertIs8DigitRecordNumber(s: String): Unit =
    assert(is8digitRecordNumber(s), s"Not an 8-digit Sierra record number: $s")

  /** Throws an exception if the given string doesn't look like a valid
    * Sierra record number.
    *
    * Note: this doesn't inspect the check digit (yet), just that it's
    * a seven- or eight-digit string.
    */
  def assertValidRecordNumber(s: String): Unit =
    assert(
      is7digitRecordNumber(s) || is8digitRecordNumber(s),
      s"Not a valid Sierra record number: $s")

  /** Increments a Sierra record number.
    *
    * For example, "1000001" -> "1000002".  This can only be used on a
    * seven-digit number; the eighth digit is a check digit, not sequential.
    */
  def increment(s: String): String = {
    assert(
      is7digitRecordNumber(s),
      s"increment() can only be used with 7-digit record numbers; not $s")

    val result = (s.toInt + 1).toString
    assertIs7DigitRecordNumber(result)
    result
  }
}

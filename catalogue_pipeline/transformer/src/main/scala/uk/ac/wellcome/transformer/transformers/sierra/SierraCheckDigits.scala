package uk.ac.wellcome.transformer.transformers.sierra

object SierraRecordTypes extends Enumeration {
  val bibs, items = Value
}

trait SierraCheckDigits {

  // Add the check digit to a Sierra ID.
  //
  // Examples:
  //
  // scala> addCheckDigit("1024364", recordType = SierraRecordTypes.bibs)
  // res0: String = b10243641
  //
  // scala> addCheckDigit("1952770", recordType = SierraRecordTypes.items)
  // res1: String = i19527706
  //
  def addCheckDigit(sierraId: String, recordType: SierraRecordTypes.Value): String = {

    // First check that we're being passed a 7-digit numeric ID -- anything
    // else is an error.
    val regexMatch = """^[0-9]{7}$""".r.unapplySeq(sierraId)
    val checkDigit = regexMatch match {
      case Some(s) => getCheckDigit(s.head)
      case _ => throw new RuntimeException(
        s"Expected 7-digit numeric ID, got $sierraId"
      )
    }

    // Then pick the record type prefix.
    val prefix = recordType match {
      case SierraRecordTypes.bibs => "b"
      case SierraRecordTypes.items => "s"
      case _ => throw new RuntimeException(
        s"Received unrecognised record type: $recordType"
      )
    }

    s"$prefix$sierraId$checkDigit"
  }

  // Quoting from the Sierra manual:
  //
  // Quoting from the Sierra manual:
  //
  //    Check digits may be any one of 11 possible digits (0, 1, 2, 3, 4, 5, 6,
  //    7, 8, 9, or x).
  //
  //    The check digit is calculated as follows:
  //
  //    Multiply the rightmost digit of the record number by 2, the next digit
  //    to the left by 3, the next by 4, etc., and total the products.
  //
  //    Divide the total by 11 and retain the remainder.  The remainder after
  //    the division is the check digit.  If the remainder is 10, the letter x
  //    is used as the check digit.
  //
  // This method does no error checking, and assumes it is passed a 7-digit
  // Sierra ID.
  //
  private def getCheckDigit(sierraId: String): String = {
    val remainder = sierraId
      .reverse
      .zip(Stream from 2)
      .map { case (char: Char, count: Int) => char.toString.toInt * count }
      .foldLeft(0)(_ + _) % 11
    if (remainder == 10) "x" else remainder.toString
  }
}

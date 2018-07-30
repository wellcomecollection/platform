package uk.ac.wellcome.models.transformable.sierra

object SierraRecordTypes extends Enumeration {
  val bibs, items = Value
}

trait SierraRecordNumber {
  val recordNumber: String

  if ("""^[0-9]{7}$""".r.unapplySeq(recordNumber) isEmpty) {
    throw new IllegalArgumentException(
      s"requirement failed: Not a 7-digit Sierra record number: $recordNumber"
    )
  }

  override def toString: String = withoutCheckDigit

  /** Returns the ID without the check digit or prefix. */
  def withoutCheckDigit: String = recordNumber
}

sealed trait SierraTypedRecordNumber extends SierraRecordNumber {
  val recordType: SierraRecordTypes.Value

  /** Returns the ID with the check digit and prefix. */
  def withCheckDigit: String = {
    val prefix = recordType match {
      case SierraRecordTypes.bibs  => "b"
      case SierraRecordTypes.items => "i"
      case _ =>
        throw new RuntimeException(
          s"Received unrecognised record type: $recordType"
        )
    }

    s"$prefix$withoutCheckDigit$getCheckDigit"
  }

  /** Returns the check digit that should be added to a record ID.
    *
    * Quoting from the Sierra manual:
    *
    * Check digits may be any one of 11 possible digits (0, 1, 2, 3, 4,
    * 5, 6, 7, 8, 9, or x).
    *
    * The check digit is calculated as follows:
    *
    * Multiply the rightmost digit of the record number by 2, the next
    * digit to the left by 3, the next by 4, etc., and total the products.
    *
    * Divide the total by 11 and retain the remainder.  The remainder
    * after the division is the check digit.  If the remainder is 10,
    * the letter x is used as the check digit.
    *
    */
  private def getCheckDigit: String = {
    val remainder = recordNumber.reverse
      .zip(Stream from 2)
      .map { case (char: Char, count: Int) => char.toString.toInt * count }
      .sum % 11
    if (remainder == 10) "x" else remainder.toString
  }
}

case class UntypedSierraRecordNumber(recordNumber: String)
    extends SierraRecordNumber

case class SierraBibNumber(recordNumber: String)
    extends SierraTypedRecordNumber {
  val recordType: SierraRecordTypes.Value = SierraRecordTypes.bibs
}

case class SierraItemNumber(recordNumber: String)
    extends SierraTypedRecordNumber {
  val recordType: SierraRecordTypes.Value = SierraRecordTypes.items
}

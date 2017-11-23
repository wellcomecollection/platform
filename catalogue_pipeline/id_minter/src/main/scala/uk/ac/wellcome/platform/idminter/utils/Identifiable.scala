package uk.ac.wellcome.platform.idminter.utils

import scala.util.Random

object Identifiable {
  private val identifierLength = 8

  private val forbiddenLetters = List('o', 'i', 'l', '1')
  private val numberRange = ('1' to '9')
  private val letterRange = ('a' to 'z')

  private val characterSet = numberRange ++ letterRange
  private val allowedCharacterSet =
    (characterSet).filterNot(forbiddenLetters.contains)
  private val firstCharacterSet =
    allowedCharacterSet.filterNot(numberRange.contains)

  def generate = {
    (1 to identifierLength)
      .map(x =>
        x match {
          // One of the serialization formats of RDF is XML, so for
          // compatibility, our identifiers have to comply with XML rules.
          // XML identifiers cannot start with numbers, so we apply the same
          // rule to the identifiers we generate.
          //
          // See: http://stackoverflow.com/a/1077111/1558022
          case 1 => firstCharacterSet(Random.nextInt(firstCharacterSet.length))
          case _ =>
            allowedCharacterSet(Random.nextInt(allowedCharacterSet.length))
      })
      .mkString
  }
}

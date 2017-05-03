package uk.ac.wellcome.platform.idminter.utils

import scala.util.Random

object Identifiable {
  private val identifierLength = 8

  private val forbiddenLetters = List('o', 'i', 'l', '1')
  private val numberRange = ('1' to '9')
  private val letterRange = ('a' to 'z')

  private val characterSet = numberRange ++ letterRange
  private val allowedCharacterSet = (characterSet).filterNot(forbiddenLetters.contains)
  private val firstCharacterSet = allowedCharacterSet.filterNot(numberRange.contains)

  def generate = {
    (1 to identifierLength)
      .map(x => x match {
        // In certain contexts, IDs are used as a string.  This can cause
        // problems if the first character is a digit, so we have a rule
        // that the first character must be a letter.
        case 1 => firstCharacterSet(Random.nextInt(firstCharacterSet.length))
        case _ => allowedCharacterSet(Random.nextInt(allowedCharacterSet.length))
      })
      .mkString
  }
}

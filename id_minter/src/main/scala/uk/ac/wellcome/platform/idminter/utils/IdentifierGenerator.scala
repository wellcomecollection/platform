package uk.ac.wellcome.platform.idminter.utils

import scala.util.Random

object Identifiable {
  val identifierLength = 8
  val forbiddenLetters = List('o', 'i', 'l', '1')
  val letterRange = ('1' to '9') ++ ('a' to 'z')
  val letterSet = letterRange.filterNot(forbiddenLetters.contains)

  def generate =
    (1 to identifierLength)
      .map(_ => letterSet(Random.nextInt(letterSet.length)))
      .mkString
}

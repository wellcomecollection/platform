package uk.ac.wellcome.models.generators

import scala.util.Random

trait RandomStrings {
  def randomAlphanumeric(length: Int): String =
    (Random.alphanumeric take length mkString) toLowerCase
}

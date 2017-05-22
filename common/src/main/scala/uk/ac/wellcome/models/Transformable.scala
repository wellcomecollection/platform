package uk.ac.wellcome.models

import scala.util.Try

trait Transformable {
  def transform: Try[Work]
}

trait Reindexable {
  val ReindexShard: String
  val ReindexVersion: Int
}
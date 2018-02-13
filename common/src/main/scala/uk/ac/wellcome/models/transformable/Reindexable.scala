package uk.ac.wellcome.models.transformable

trait Reindexable {
  val id: String
  val reindexShard: String
  val reindexVersion: Int
}

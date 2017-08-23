package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Work

import scala.util.Try

trait Transformable {
  def transform: Try[Work]
}

case class HashKey(keyName: String, keyValue: String)
case class RangeKey[T](keyName: String, keyValue: T)
case class ItemIdentifier[T](hashKey: HashKey, rangeKey: RangeKey[T])

trait Reindexable[T] {
  val id: ItemIdentifier[T]
  val ReindexShard: String
  val ReindexVersion: Int
}

object Reindexable {
  def getReindexItem[T](reindexable: Reindexable[T]) =
    ReindexItem(reindexable.id,
                reindexable.ReindexShard,
                reindexable.ReindexVersion)
}

case class ReindexItem[T](id: ItemIdentifier[T],
                          ReindexShard: String,
                          ReindexVersion: Int)
    extends Reindexable[T] {

  def hashKey = Symbol(id.hashKey.keyName) -> id.hashKey.keyValue
  def rangeKey = Symbol(id.rangeKey.keyName) -> id.rangeKey.keyValue
}

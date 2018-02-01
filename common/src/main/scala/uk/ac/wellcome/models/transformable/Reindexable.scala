package uk.ac.wellcome.models.transformable

trait Reindexable[T] {
  val reindexId: ItemIdentifier[T]
  val ReindexShard: String
  val ReindexVersion: Int
}

object Reindexable {
  def getReindexItem[T](reindexable: Reindexable[T]) =
    ReindexItem(reindexable.reindexId,
                reindexable.ReindexShard,
                reindexable.ReindexVersion)
}

case class HashKey(keyName: String, keyValue: String)
case class RangeKey[T](keyName: String, keyValue: T)
case class ItemIdentifier[T](hashKey: HashKey, rangeKey: RangeKey[T])

case class ReindexItem[T](reindexId: ItemIdentifier[T],
                          ReindexShard: String,
                          ReindexVersion: Int)
    extends Reindexable[T] {

  def hashKey = Symbol(reindexId.hashKey.keyName) -> reindexId.hashKey.keyValue
  def rangeKey =
    Symbol(reindexId.rangeKey.keyName) -> reindexId.rangeKey.keyValue
}

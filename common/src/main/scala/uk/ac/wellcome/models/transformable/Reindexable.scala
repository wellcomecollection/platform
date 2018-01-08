package uk.ac.wellcome.models.transformable

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

case class HashKey(keyName: String, keyValue: String)
case class RangeKey[T](keyName: String, keyValue: T)
case class ItemIdentifier[T](hashKey: HashKey, rangeKey: RangeKey[T])


case class ReindexItem[T](id: ItemIdentifier[T],
                          ReindexShard: String,
                          ReindexVersion: Int)
  extends Reindexable[T] {

  def hashKey = Symbol(id.hashKey.keyName) -> id.hashKey.keyValue
  def rangeKey = Symbol(id.rangeKey.keyName) -> id.rangeKey.keyValue
}
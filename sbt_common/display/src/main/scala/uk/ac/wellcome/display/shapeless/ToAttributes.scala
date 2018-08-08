package uk.ac.wellcome.display.shapeless

import shapeless.{HList, LabelledGeneric, Poly1}
import shapeless.ops.hlist.{Mapper, ToTraversable}
import shapeless.ops.record.Keys
import shapeless.tag.Tagged

trait ToAttributes[T] {
  def toAttributes: List[String]
}

object ToAttributes {
  object symbolName extends Poly1 {
    implicit def atTaggedSymbol[T] = at[Symbol with Tagged[T]](_.name)
  }

  implicit def familyFormat[T, Repr <: HList, KeysRepr <: HList, MapperRepr <: HList](
                                                                                       implicit gen: LabelledGeneric.Aux[T, Repr],
                                                                                       keys: Keys.Aux[Repr, KeysRepr],
                                                                                       mapper: Mapper.Aux[symbolName.type, KeysRepr, MapperRepr],
                                                                                       traversable: ToTraversable.Aux[MapperRepr, List, String]
                                                                                     ): ToAttributes[T] = new ToAttributes[T] {
    def toAttributes: List[String] = {
      identity(gen)
      keys().map(symbolName).toList
    }
  }

  def toAttributes[T](implicit c: ToAttributes[T]): List[String] = c.toAttributes
}

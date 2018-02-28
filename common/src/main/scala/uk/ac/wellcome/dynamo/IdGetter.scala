package uk.ac.wellcome.dynamo

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector
import shapeless.record._


trait IdGetter[T] {
  def id(t: T): String
}

object IdGetter {
  val w = Witness(Symbol("id"))
  type id = w.T

  def apply[A](implicit enc: IdGetter[A]): IdGetter[A] =
    enc

  def createIdGetter[T](f: T => String): IdGetter[T] = new IdGetter[T] {
    def id(t: T) = f(t)
  }

  implicit def elementIdGetter[L <: HList](implicit selector: Selector.Aux[FieldType[id, String] :: L, id, String]) = createIdGetter {t: (FieldType[id, String] :: L) =>
    t.get('id)
  }

  implicit def hlistIdGetter[H, T <: HList](implicit idGetter: IdGetter[T]) = createIdGetter[H::T]{t: (H::T)=>
    idGetter.id(t.tail)
  }

  implicit def productIdGetter[C, T](implicit labelledGeneric: LabelledGeneric.Aux[C,T],idGetter: IdGetter[T]) = createIdGetter[C]{t: C =>
    idGetter.id(labelledGeneric.to(t))
  }
}
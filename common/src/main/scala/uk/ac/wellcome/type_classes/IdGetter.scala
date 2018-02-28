package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector
import shapeless.record._

// Type class that returns the id of an instances of T
trait IdGetter[T] {
  def id(t: T): String
}

object IdGetter {
  val w = Witness(Symbol("id"))
  type IdKey = w.T

  type IdField = FieldType[IdKey, String]

  def apply[A](implicit enc: IdGetter[A]): IdGetter[A] =
    enc

  def createIdGetter[T](f: T => String): IdGetter[T] = new IdGetter[T] {
    def id(t: T) = f(t)
  }

  // Generates an IdGetter for an HList of arbitrary length with the id field as the first field
  implicit def elementIdGetter[L <: HList](
    implicit selector: Selector.Aux[IdField :: L, IdKey, String]) =
    createIdGetter { t: IdField :: L =>
      t.get('id)
    }

  // Generates an IdGetter for an HList returning the IdGetter for the tail of the HList
  implicit def hlistIdGetter[H, T <: HList](implicit idGetter: IdGetter[T]) =
    createIdGetter[H :: T] { t: (H :: T) =>
      idGetter.id(t.tail)
    }

  // Generates an IdGetter for a case class using the IdGetter for its HLists representation
  implicit def productIdGetter[C, T](
    implicit labelledGeneric: LabelledGeneric.Aux[C, T],
    idGetter: IdGetter[T]) = createIdGetter[C] { t: C =>
    idGetter.id(labelledGeneric.to(t))
  }
}

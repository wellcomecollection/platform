package uk.ac.wellcome.storage.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector
import shapeless.record._

// Type class with a method for getting the ID of an instance of T.
//
// The type T must have a field "id".
//
// This isn't defined by the type hierarchy in the trait; instead it's checked
// at compile-time by shapeless and the companion object.
//
trait IdGetter[T] {
  def id(t: T): String
}

object IdGetter {
  val w = Witness(Symbol("id"))
  type IdKey = w.T

  def apply[A](implicit enc: IdGetter[A]): IdGetter[A] =
    enc

  def createIdGetter[T](f: T => String): IdGetter[T] = new IdGetter[T] {
    def id(t: T) = f(t)
  }

  // Generates an IdGetter for an HList.
  //
  // Selector is a Shapeless type class that selects an individual field
  // from an HList.  It takes three type parameters:
  //
  //  - the type of the input (L)
  //  - the field to select (IdKey)
  //  - the type of the returned value (String)
  //
  implicit def hlistIdGetter[L <: HList](
    implicit selector: Selector.Aux[L, IdKey, String]) =
    createIdGetter { t: L =>
      selector(t)
    }

  // Generates an IdGetter for a case class ("product" in Shapeless).
  //
  // LabelledGeneric is a Shapeless type class that allows us to convert
  // between case classes and their HList representation.
  //
  // labelledGeneric.to(c) converts the case class C to an HList L, and then we
  // can use the constructor above.
  //
  implicit def productIdGetter[C, L <: HList](
    implicit labelledGeneric: LabelledGeneric.Aux[C, L],
    idGetter: IdGetter[L]) = createIdGetter[C] { c: C =>
    idGetter.id(labelledGeneric.to(c))
  }
}
